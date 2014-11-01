We use the standard Java Service Loader interface. All you need to do is 

1. Implement `com.alexecollins.docker.orchestration.api.Plugin`.
2. Create `META-INF/services/com.alexecollins.docker.orchestration.api.Plugin` with a single line with your plugin's class name. 