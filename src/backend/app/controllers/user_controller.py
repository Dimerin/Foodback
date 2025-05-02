from flask import Blueprint, request, jsonify, make_response
from services import UserService
from jsonschema import validate, ValidationError
from logger import log
import json
from exceptions import UserAlreadyExistsError
from flask_jwt_extended import verify_jwt_in_request, get_jwt


class UserController:
    def __init__(self):
        self.user_service = UserService()
        self.bp = Blueprint('user_bp', __name__)
        self._register_routes()

    def _register_routes(self):
        self.bp.add_url_rule(
            '/users/<int:user_id>', 
            view_func=self.get_user, 
            methods=['GET']
            )
        
        self.bp.add_url_rule(
            '/users/signup', 
            view_func=self.register_user, 
            methods=['POST']
            )
        self.bp.add_url_rule(
            '/users', 
            view_func=self.get_users, 
            methods=['GET']
            )
    
    def get_users(self):
        
        log.info("API: Get Users")
        try:
            verify_jwt_in_request()
            claims = get_jwt()
            if claims.get('user_type') != 'admin':
                return make_response(
                    jsonify({"error": "Unauthorized"}), 401
                )
        except Exception as e:
            log.error(f"JWT verification failed: {e}")
            return make_response(
                jsonify({"error": "Unauthorized"}), 401
            )
            
        users = self.user_service.get_all_users()
        return make_response(
            jsonify([{
                "id": user.id,
                "name": user.name,
                "surname": user.surname,
                "user_type": user.user_type,
                "dateOfBirth": user.dateOfBirth.isoformat(),
                "email": user.email
            } for user in users]), 200
        )
    
    def get_user(self, user_id):
        try:
            verify_jwt_in_request()
            claims = get_jwt()
            if claims.get('user_type') != 'admin':
                return make_response(
                    jsonify({"error": "Unauthorized"}), 401
                )
        except Exception as e:
            log.error(f"JWT verification failed: {e}")
            return make_response(
                jsonify({"error": "Unauthorized"}), 401
            )
        user = self.user_service.get_user_by_id(user_id)
        if user:
            return make_response(
                jsonify({
                    "id": user.id,
                    "name": user.name,
                    "surname": user.surname,
                    "dateOfBirth": user.dateOfBirth.isoformat(),
                    "email": user.email,
                    "user_type": user.user_type
                }), 200
            )
        return make_response(
            jsonify({"error": "User not found"}), 404
        )

    def register_user(self):
        log.info("API: User Registration")
        
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
                    "id": user.id,
                    "name": user.name,
                    "surname": user.surname,
                    "user_type": user.user_type,
                    "dateOfBirth": user.dateOfBirth.isoformat(),
                    "email": user.email
                }), 201
            )
        except UserAlreadyExistsError as e:
            log.error(f"User already exists: {e}")
            return make_response(
                jsonify({"error": "Registration failed"}), 409
            )
        except Exception as e:
            log.error(f"Error creating user: {e}")
            return make_response(
                jsonify({"error": "User registration failed"}), 500
            )
