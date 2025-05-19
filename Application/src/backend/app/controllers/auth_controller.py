import json
from flask_jwt_extended import create_access_token, create_refresh_token
from flask_jwt_extended import get_jwt_identity, verify_jwt_in_request, get_jwt
from flask import Blueprint, request, jsonify, make_response
from services import AuthService, UserService
from jsonschema import validate, ValidationError
from exceptions import InvalidCredentialsError, UserNotFoundError, UserAlreadyExistsError

class AuthController:
    def __init__(self):
        self.auth_service = AuthService()
        self.user_service = UserService()
        self.bp = Blueprint('auth_bp', __name__)
        self._register_routes()

    def _register_routes(self):
        self.bp.add_url_rule(
            '/auth/signup',
            view_func=self.register_user,
            methods=['POST']
        )
        self.bp.add_url_rule(
            '/auth/login',
            view_func=self.login,
            methods=['POST']
        )
        self.bp.add_url_rule(
            '/auth/me',
            view_func=self.get_current_user,
            methods=['GET']
        )
        self.bp.add_url_rule(
            '/auth/refresh',
            view_func=self.refresh_token,
            methods=['POST']
        )

    def register_user(self):
        
        data = request.get_json()
        if not data:
            return make_response(jsonify({"error": "No input data provided"}), 400)
        
        schema_path = 'schemas/registration_schema.json'
        with open(schema_path, 'r') as schema_file:
            schema = json.load(schema_file)
        try:
            validate(instance=data, schema=schema)
        except ValidationError as e:    
            return make_response(jsonify({"error": e.message}), 400)
        except FileNotFoundError:
            return make_response(jsonify({"error": "Schema file not found"}), 500)

        # Extracting data from the request        
        name = data.get('name')
        surname = data.get('surname')
        dateOfBirth = data.get('dateOfBirth')  # ISO 8601 string (e.g. "1990-01-01")
        email = data.get('email')
        password = data.get('password')
        gender = data.get('gender')
        
        try:
            user = self.user_service.create_user(
                name, surname, dateOfBirth, gender, email, password
            )
            return make_response(
                jsonify({
                    "id": user.id
                }), 201
            )
        except UserAlreadyExistsError as e:
            return make_response(
                jsonify({"error": "Email already registered"}), 409
            )
        except Exception as e:
            return make_response(
                jsonify({"error": "User registration failed"}), 500
            )
        
    def login(self):
        data = request.get_json()
        if not data:
            return make_response(jsonify({"error": "No input data provided"}), 400)

        try:
            schema_path = 'schemas/login_schema.json'
            with open(schema_path, 'r') as schema_file:
                schema = json.load(schema_file)
            validate(instance=data, schema=schema)
        except ValidationError as e:
            return make_response(jsonify({"error": e.message}), 400)
        except FileNotFoundError:
            return make_response(jsonify({"error": "Schema file not found"}), 500)
        
        email = data.get('email')
        password = data.get('password')

        try:
            user = self.auth_service.authenticate_user(email, password)
            if not user:
                return make_response(jsonify({"error": "Invalid credentials"}), 401)
        except InvalidCredentialsError:
            return make_response(jsonify({"error": "Invalid credentials"}), 401)            
        except UserNotFoundError:
            return make_response(jsonify({"error": "User not found"}), 404)
        except Exception as e:
            return make_response(jsonify({"error": str(e)}), 500)

        access_token = create_access_token(
                identity=str(user.id), 
                fresh=True,
                additional_claims={"user_type": user.user_type}
            )
        refresh_token = create_refresh_token(identity=str(user.id))
        
        return make_response(jsonify({
            "id": user.id,
            "name": user.name,
            "surname": user.surname,
            "email": user.email,
            "user_type": user.user_type,
            "token_type": "Bearer",
            "access_token": access_token,
            "refresh_token": refresh_token
        }), 200)

    def get_current_user(self):
        try:
            verify_jwt_in_request()
        except Exception as e:
            return make_response(jsonify({"error": str(e)}), 401)
        current_user_id = get_jwt_identity()
        # Getting the current user
        current_user = self.user_service.get_user_by_id(current_user_id)
        if not current_user:
            return make_response(jsonify({"error": "User not found"}), 404)

        # Same response as in the login        
        return make_response(jsonify({
            "id": current_user.id,
            "name": current_user.name,
            "surname": current_user.surname,
            "email": current_user.email,
            "user_type": current_user.user_type
        }), 200)        


    def refresh_token(self):
        try:
            verify_jwt_in_request(refresh=True)
        except Exception as e:
            return make_response(jsonify({"error": str(e)}), 401)
        current_user = get_jwt_identity()
        new_access_token = create_access_token(
            identity=current_user,
            fresh=False,
            additional_claims={"user_type": get_jwt().get("user_type")}    
        )
        
        return make_response(jsonify({
            "access_token": new_access_token,
            "token_type": "Bearer"
        }), 200)