package ch.arka.projecthub

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.arka.projecthub.ui.add.AddProjectScreen
import ch.arka.projecthub.ui.home.HomeScreen
import ch.arka.projecthub.ui.project.ProjectScreen
import ch.arka.projecthub.ui.theme.ProjectHubTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_ADD = "add"
private const val ARG_PROJECT_ID = "id"
private const val ROUTE_PROJECT = "project/{$ARG_PROJECT_ID}"

private fun projectRoute(id: Long) = "project/$id"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Il tema dell'app è sempre scuro: le icone di sistema devono restare chiare
        // anche se il telefono è in tema chiaro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            ProjectHubTheme { ProjectHubApp() }
        }
    }
}

@Composable
private fun ProjectHubApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val projects by vm.projects.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                projects = projects,
                onOpen = { id -> nav.navigate(projectRoute(id)) },
                onAdd = { nav.navigate(ROUTE_ADD) },
            )
        }
        composable(
            route = ROUTE_PROJECT,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong(ARG_PROJECT_ID)
            ProjectScreen(
                project = projects.firstOrNull { it.project.id == id },
                onBack = { nav.popBackStack() },
                onToggleTask = vm::setTaskDone,
                onDelete = {
                    if (id != null) vm.delete(id)
                    nav.popBackStack()
                },
            )
        }
        composable(ROUTE_ADD) {
            AddProjectScreen(
                onBack = { nav.popBackStack() },
                onCreate = { name, goal, category, deadline ->
                    vm.create(name, goal, category, deadline) { id ->
                        // "indietro" dal nuovo progetto torna alla home, non al modulo
                        nav.navigate(projectRoute(id)) {
                            popUpTo(ROUTE_HOME)
                        }
                    }
                },
            )
        }
    }
}
