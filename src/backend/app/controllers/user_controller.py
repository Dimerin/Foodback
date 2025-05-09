from flask import Blueprint, request, jsonify, make_response
from services import UserService
from jsonschema import validate, ValidationError
from logger import log
import json
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

    
