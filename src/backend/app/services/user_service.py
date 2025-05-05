from repositories import UserRepository
from werkzeug.security import generate_password_hash
from exceptions import UserAlreadyExistsError

class UserService:
    def __init__(self):
        self.user_repository = UserRepository()

    def create_user(self, name, surname, dateOfBirth, gender,email, password):
        if self.user_repository.get_by_email(email):
            raise UserAlreadyExistsError("User with email already exists.")
        
        hashed_password = generate_password_hash(password)
        return self.user_repository.create(
            name, surname, dateOfBirth, gender, email, hashed_password
            )

    def get_user_by_id(self, user_id):
        return self.user_repository.get_by_id(user_id)
    
    def get_all_users(self):
        return self.user_repository.get_all_users()