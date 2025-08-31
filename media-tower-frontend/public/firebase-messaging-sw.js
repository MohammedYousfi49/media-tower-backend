// Ce fichier doit être à la racine du dossier `public`

importScripts("https://www.gstatic.com/firebasejs/9.22.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/9.22.1/firebase-messaging-compat.js");

// Vos clés Firebase sont intégrées directement
const firebaseConfig = {
    apiKey: "AIzaSyA4wf3VKC-IO16JCXuanPAMFw9KB72uBPI",
    authDomain: "mediatower-backend.firebaseapp.com",
    projectId: "mediatower-backend",
    storageBucket: "mediatower-backend.appspot.com", // Correction
    messagingSenderId: "498973673308",
    appId: "1:498973673308:web:32422caa9017995f832399"
};

firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    console.log(
        "[firebase-messaging-sw.js] Received background message ",
        payload
    );

    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: "/logo.png", // Assurez-vous d'avoir un logo.png dans votre dossier public
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});