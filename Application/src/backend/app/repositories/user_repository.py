from models import User
from core import db

class UserRepository:
    def create(
        self, name, surname, dateOfBirth,
        gender, email, password, user_type='user'
        ):
        new_user = User(
            name=name,
            surname=surname,
            dateOfBirth=dateOfBirth,
            gender=gender,
            email=email,
            password=password,
            user_type=user_type
        )
        db.session.add(new_user)
        db.session.commit()
        return new_user

    def get_by_id(self, user_id):
        return User.query.get(user_id)

    def get_by_email(self, email):
        return User.query.filter_by(email=email).first()

    def get_all_users(self):
        return User.query.filter(User.user_type != "admin").all()