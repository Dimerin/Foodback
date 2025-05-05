
class UserAlreadyExistsError(Exception):
    """Eccezione sollevata quando l'utente esiste già nel sistema."""
    pass

class UserNotFoundError(Exception):
    """Eccezione sollevata quando l'utente non viene trovato nel sistema."""
    pass

