from repositories import UserRepository
from werkzeug.security import check_password_hash
from exceptions import UserNotFoundError
from models import User

class AuthService:
    def __init__(self):
        self.user_repository = UserRepository()
        
    def authenticate_user(self, email, password) -> User | None:
        
        user = self.user_repository.get_by_email(email)
        
        if not user:
            raise UserNotFoundError(f"User with email {email} not found.")
        
        if user and check_password_hash(user.password, password):
            return user
        return None
