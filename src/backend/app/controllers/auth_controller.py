import json
from flask_jwt_extended import create_access_token, create_refresh_token
from flask_jwt_extended import get_jwt_identity, verify_jwt_in_request, get_jwt
from flask import Blueprint, request, jsonify, make_response
from services import AuthService
from jsonschema import validate, ValidationError
from exceptions import InvalidCredentialsError, UserNotFoundError

class AuthController:
    def __init__(self):
        self.auth_service = AuthService()
        self.bp = Blueprint('auth_bp', __name__)
        self._register_routes()

    def _register_routes(self):
        self.bp.add_url_rule(
            '/auth/login',
            view_func=self.login,
            methods=['POST']
        )
        self.bp.add_url_rule(
            '/auth/refresh',
            view_func=self.refresh_token,
            methods=['POST']
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
            "access_token": access_token,
            "refresh_token": refresh_token,
            "token_type": "Bearer",
            "user": {
                "id": user.id,
                "email": user.email
            }
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