CREATE DATABASE IF NOT EXISTS foodback_database;

USE foodback_database;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    dateOfBirth DATE NOT NULL,
    gender ENUM('Male', 'Female') NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE, 
    password VARCHAR(255) NOT NULL,
    user_type ENUM('admin', 'user') NOT NULL DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (name, surname, dateOfBirth, gender, email, password, user_type)
VALUES (
    'Admin', 'FoodBack', '1990-01-01', 'male',
    'admin', 
    'scrypt:32768:8:1$ZSsde058VzygOIM5$035c36d29e21d7d39886a121d164f3fd5652652a93dde2c033a06a1c4b690972d09bd422ff9ad089d05f9aad26e87c5552086158faf95108e6fba955104e2225',
    'admin'
    );
