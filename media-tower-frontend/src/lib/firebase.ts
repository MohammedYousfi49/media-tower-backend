import { initializeApp } from "firebase/app";
// --- CORRECTION : Importer les fonctions de persistance ---
import { getAuth, setPersistence, browserSessionPersistence } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getMessaging, getToken, onMessage } from "firebase/messaging";
import axiosClient from "../api/axiosClient";

const firebaseConfig = {
    apiKey: "AIzaSyA4wf3VKC-IO16JCXuanPAMFw9KB72uBPI",
    authDomain: "mediatower-backend.firebaseapp.com",
    projectId: "mediatower-backend",
    storageBucket: "mediatower-backend.appspot.com",
    messagingSenderId: "498973673308",
    appId: "1:498973673308:web:32422caa9017995f832399"
};

const VAPID_KEY = "BIcMBlEMObl_rV4ac7KgbJPW_t-I87aO18l6NAyEKSFz4jYzXsaqQszAuff7UElxRutj4xBwAhHUJT1YUX-Pguo";

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const storage = getStorage(app);
const messaging = getMessaging(app);

// --- CORRECTION MAJEURE : On initialise Auth ET on active la persistance ---
const auth = getAuth(app);
setPersistence(auth, browserSessionPersistence)
    .catch((error) => {
        console.error("Firebase Auth persistence error:", error.code, error.message);
    });
// -----------------------------------------------------------------------

export const requestNotificationPermission = async () => {
    try {
        const permission = await Notification.requestPermission();
        if (permission === "granted") {
            console.log("Notification permission granted.");
            const currentToken = await getToken(messaging, {
                vapidKey: VAPID_KEY,
            });
            if (currentToken) {
                console.log("FCM Token:", currentToken);
                await axiosClient.post("/users/fcm-token", { fcmToken: currentToken });
            } else {
                console.log("No registration token available.");
            }
        } else {
            console.log("Unable to get permission to notify.");
        }
    } catch (err) {
        console.error("An error occurred while retrieving token.", err);
    }
};

onMessage(messaging, (payload) => {
    console.log("Message received in foreground: ", payload);
});

export { auth, db, storage, messaging };