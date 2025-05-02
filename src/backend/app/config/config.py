import os
from datetime import timedelta

class Config:
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_NAME = os.getenv("DB_NAME", "foodback_database")
    SQLALCHEMY_DATABASE_URI = (
        f"mysql+pymysql://{os.getenv('DB_USER')}:{os.getenv('DB_PASSWORD')}"
        f"@{os.getenv('DB_HOST')}:3306/{os.getenv('DB_NAME')}"
    )
    SECRET_KEY = os.getenv("SECRET_KEY", "secret_key")
    JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY")
    
    JWT_TOKEN_LOCATION = ["headers", "cookies"]
    JWT_COOKIE_SECURE = False
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(hours=1)
    JWT_REFRESH_TOKEN_EXPIRES = timedelta(days=30) 
    