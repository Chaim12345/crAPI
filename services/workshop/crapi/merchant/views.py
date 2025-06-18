#
# Licensed under the Apache License, Version 2.0 (the “License”);
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an “AS IS” BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""
contains all the views related to Merchant
"""
import logging
from urllib.parse import urlparse # Added import
import requests
from requests.exceptions import MissingSchema, InvalidURL
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from crapi.merchant.serializers import ContactMechanicSerializer
from utils.jwt import jwt_auth_required
from utils import messages
from utils.logging import log_error

logger = logging.getLogger()


class ContactMechanicView(APIView):
    """
    View for contact mechanic feature
    """
    @jwt_auth_required
    def post(self, request, user=None):
        """
        contact_mechanic view to call the mechanic api
        :param request: http request for the view
            method allowed: POST
            http request should be authorised by the jwt token of the user
            mandatory fields: ['mechanic_api', 'repeat_request_if_failed', 'number_of_repeats']
        :param user: User object of the requesting user
        :returns Response object with
            response_from_mechanic_api and 200 status if no error
            message and corresponding status if error
        """
        request_data = request.data
        serializer = ContactMechanicSerializer(data=request_data)
        if not serializer.is_valid():
            log_error(request.path, request.data, status.HTTP_400_BAD_REQUEST, serializer.errors)
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        # SSRF Vulnerability Fix: Validate mechanic_api URL
        mechanic_api_url = request_data['mechanic_api']
        try:
            parsed_url = urlparse(mechanic_api_url)
            if parsed_url.scheme not in ['http', 'https']:
                log_error(request.path, request.data, status.HTTP_400_BAD_REQUEST, "Invalid URL scheme.")
                return Response({'message': 'Invalid URL scheme. Only http and https are allowed.'}, status=status.HTTP_400_BAD_REQUEST)

            # Define an allow-list for hostnames
            # For crAPI, typically services run in Docker and might be referenced by service name or localhost.
            # Allowing 'localhost' and '127.0.0.1' covers calls to itself or services on the same network interface.
            # If specific internal service names were used (e.g., 'crapi-mechanic-service'), they'd be added here.
            allowed_hostnames = ['localhost', '127.0.0.1']
            # Example if the service could call itself via its externally exposed host:
            # current_service_host = request.get_host().split(':')[0]
            # if current_service_host not in allowed_hostnames:
            #     allowed_hostnames.append(current_service_host)

            if parsed_url.hostname not in allowed_hostnames:
                log_error(request.path, request.data, status.HTTP_400_BAD_REQUEST, f"Invalid hostname: {parsed_url.hostname}")
                return Response({'message': f"Invalid hostname: {parsed_url.hostname}. Access to this host is not allowed."}, status=status.HTTP_400_BAD_REQUEST)

        except Exception as e: # Catch any parsing errors
            log_error(request.path, request.data, status.HTTP_400_BAD_REQUEST, f"Invalid URL format: {str(e)}")
            return Response({'message': f"Invalid URL format: {str(e)}"}, status=status.HTTP_400_BAD_REQUEST)
        # End of SSRF Vulnerability Fix

        repeat_request_if_failed = request_data['repeat_request_if_failed']
        number_of_repeats = request_data['number_of_repeats']
        if repeat_request_if_failed and number_of_repeats < 1:
            return Response(
                {'message': messages.MIN_NO_OF_REPEATS_FAILED},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )
        elif repeat_request_if_failed and number_of_repeats > 100:
            return Response(
                {'message': messages.NO_OF_REPEATS_EXCEEDED},
                status=status.HTTP_503_SERVICE_UNAVAILABLE
            )

        repeat_count = 0
        while True:
            request_url=request_data['mechanic_api']
            logger.info(f"Repeat count: {repeat_count}, mechanic_api: {request_url}")
            try:
                mechanic_response = requests.get(
                    request_url, # This should be mechanic_api_url after validation, which it is as request_data['mechanic_api'] is used.
                    params=request_data,
                    headers={'Authorization': request.META.get('HTTP_AUTHORIZATION')}
                    # verify=False removed to enable SSL verification by default
                )
                if mechanic_response.status_code == status.HTTP_200_OK:
                    logger.info(f"Got a valid response at repeat count: {repeat_count}")
                    break
                if not repeat_request_if_failed:
                    break
                if repeat_count == number_of_repeats:
                    break
                repeat_count += 1
            except (MissingSchema, InvalidURL) as e:
                log_error(request.path, request.data, status.HTTP_400_BAD_REQUEST, e)
                return Response({'message': str(e)}, status=status.HTTP_400_BAD_REQUEST)
            except requests.exceptions.ConnectionError as e:
                if not repeat_request_if_failed:
                    return Response(
                        {'message': messages.COULD_NOT_CONNECT},
                        status=status.HTTP_400_BAD_REQUEST
                    )
                if repeat_count == number_of_repeats:
                    return Response(
                        {'message': messages.COULD_NOT_CONNECT},
                        status=status.HTTP_400_BAD_REQUEST
                    )
                repeat_count += 1
                continue
        mechanic_response_status = mechanic_response.status_code
        try:
            mechanic_response = mechanic_response.json()
        except ValueError:
            mechanic_response = mechanic_response.text
        return Response({
            'response_from_mechanic_api': mechanic_response,
            'status': mechanic_response_status
        }, status=mechanic_response_status)
