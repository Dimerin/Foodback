class InvalidCredentialsError(Exception):
    """Eccezione sollevata quando le credenziali fornite non sono valide."""
    pass

class TokenExpiredError(Exception):
    """Eccezione sollevata quando il token di accesso è scaduto."""
    pass