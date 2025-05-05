from flask import Flask
from flask_jwt_extended import JWTManager
from core import db
from config.config import Config
from controllers import UserController, AuthController

def main():
    app = Flask(__name__)
    
    app.config.from_object(Config)
    
    JWTManager(app)
    
    db.init_app(app)
    
    user_controller = UserController()
    auth_controller = AuthController()
    app.register_blueprint(user_controller.bp)
    app.register_blueprint(auth_controller.bp)
    
    with app.app_context():
        db.create_all()     
        
    app.run(host="0.0.0.0", port=5000, debug=True)
    
if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("[ERROR] Server stopped by user.")
    except Exception as e:
        print(f"[ERROR] An error occurred: {e}")