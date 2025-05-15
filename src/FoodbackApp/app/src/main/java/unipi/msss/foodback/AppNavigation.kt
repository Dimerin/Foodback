package unipi.msss.foodback

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import unipi.msss.foodback.auth.login.ui.LoginView
import unipi.msss.foodback.home.ui.HomeView
import unipi.msss.foodback.auth.signup.ui.SignUpView
import unipi.msss.foodback.home.ui.TastingView

sealed class NavDestinations(val route: String) {
    object Login : NavDestinations("login")
    object Home : NavDestinations("home")
    object SignUp : NavDestinations("signup")
    object DataCollection : NavDestinations("datacollection")
}

@Composable
fun AppNavigator(navController: NavHostController, authState: AuthState) {
    val startDestination = when (authState) {
        AuthState.Authenticated -> NavDestinations.Home.route
        AuthState.AdminAuthenticated -> NavDestinations.DataCollection.route
        else -> NavDestinations.DataCollection.route // FIXME: Put Login before release
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavDestinations.Login.route) {
            LoginView(
                onLoginSuccess = {
                    navController.navigate(NavDestinations.Home.route) {
                        popUpTo(NavDestinations.Login.route) { inclusive = true }
                    }
                },
                onLoginAdminSuccess = {
                    navController.navigate(NavDestinations.DataCollection.route) {
                        popUpTo(NavDestinations.Login.route) { inclusive = true }
                    }
                },
                onSignUpClicked = {
                    navController.navigate(NavDestinations.SignUp.route)
                },
            )
        }

        composable(NavDestinations.Home.route) {
            HomeView(
                onLoggedOut = {
                    navController.navigate(NavDestinations.Login.route) {
                        popUpTo(0) // Clear everything from the back stack
                    }
                }
            )
        }

        composable(NavDestinations.SignUp.route) {
            SignUpView(
                onSignUpSuccess = {
                    navController.navigate(NavDestinations.Login.route) {
                        popUpTo(NavDestinations.SignUp.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(NavDestinations.DataCollection.route) {
            TastingView(
                onLoggedOut = {
                    navController.navigate(NavDestinations.Login.route) {
                        popUpTo(0) // Clear everything from the back stack
                    }
                }
            )
        }
    }
}

