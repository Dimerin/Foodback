package unipi.msss.foodback

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import unipi.msss.foodback.auth.login.ui.LoginView
import unipi.msss.foodback.home.ui.HomeView
import unipi.msss.foodback.auth.signup.ui.SignUpView
import unipi.msss.foodback.home.ui.TastingView
import unipi.msss.foodback.home.ui.TastingViewModel

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
        // else -> NavDestinations.Login.route
        else -> NavDestinations.DataCollection.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavDestinations.Login.route) {
            LoginView(
                onLoginSuccess = {
                    navController.navigate(NavDestinations.Home.route) {
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
                },
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
            val viewModel: TastingViewModel = hiltViewModel()
            TastingView(

            )
        }
    }
}

