// Fichier : src/main/java/com/mediatower/backend/security/FirebaseConfig.java (APPROCHE ALTERNATIVE)

package com.mediatower.backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // On définit les constantes pour le chemin du fichier et l'ID du projet
    private static final String SERVICE_ACCOUNT_KEY_PATH = "serviceAccountKey.json";
    private static final String FIREBASE_PROJECT_ID = "mediatower-backend"; // Assurez-vous que c'est le bon

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // On vérifie si une application Firebase est déjà initialisée
        if (FirebaseApp.getApps().isEmpty()) {
            System.out.println("Initializing Firebase Admin SDK...");

            // On charge le fichier de clé depuis le classpath (src/main/resources)
            InputStream serviceAccount = new ClassPathResource(SERVICE_ACCOUNT_KEY_PATH).getInputStream();

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .build();

            FirebaseApp.initializeApp(options);
            System.out.println("Firebase Admin SDK initialized successfully.");
        } else {
            System.out.println("Firebase Admin SDK already initialized.");
        }
        // On retourne l'instance principale
        return FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        // On s'assure d'obtenir l'instance Firestore à partir de l'application initialisée
        System.out.println("Providing Firestore bean...");
        return FirestoreClient.getFirestore(firebaseApp);
    }
}