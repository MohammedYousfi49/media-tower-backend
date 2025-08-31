import { useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createUserWithEmailAndPassword } from 'firebase/auth';
// --- CORRECTION DU CHEMIN D'IMPORT ---
import { auth } from '../../lib/firebase';
import axiosClient from '../../api/axiosClient';
import { FirebaseError } from 'firebase/app';
import { AxiosError } from 'axios';
// --- CORRECTION DU CHEMIN D'IMPORT ---
import { useAuth, AppUser } from '../../contexts/AuthContext';
import { Loader2 } from 'lucide-react';

const Register = () => {
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const { setAppUser } = useAuth();

    const handleRegister = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        const form = e.currentTarget;
        const firstName = (form.elements.namedItem('firstName') as HTMLInputElement).value;
        const lastName = (form.elements.namedItem('lastName') as HTMLInputElement).value;
        const email = (form.elements.namedItem('email') as HTMLInputElement).value;
        const password = (form.elements.namedItem('password') as HTMLInputElement).value;

        if (password.length < 6) {
            setError('Password must be at least 6 characters long.');
            setLoading(false);
            return;
        }

        try {
            const userCredential = await createUserWithEmailAndPassword(auth, email, password);
            const firebaseUser = userCredential.user;

            const registrationData = {
                uid: firebaseUser.uid,
                email: email,
                password: password,
                firstName: firstName,
                lastName: lastName,
            };

            const response = await axiosClient.post<AppUser>('/auth/register', registrationData);
            setAppUser(response.data);
            navigate('/auth-redirect');

        } catch (err) {
            if (err instanceof FirebaseError && err.code === 'auth/email-already-in-use') {
                setError('This email is already registered. Please log in.');
            } else if (err instanceof AxiosError && err.response) {
                const responseData = err.response.data as { message?: string } | string;
                setError(typeof responseData === 'string' ? responseData : responseData.message || 'Registration failed.');
            } else {
                setError('An unexpected error occurred during registration.');
            }
            console.error("Registration failed:", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-900 text-white">
            <div className="w-full max-w-md p-8 space-y-6 bg-card rounded-lg shadow-md">
                <h1 className="text-2xl font-bold text-center text-foreground">Create an Account</h1>
                {error && <p className="text-red-500 text-center p-2 bg-red-900/20 rounded-md">{error}</p>}
                <form onSubmit={handleRegister} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-400">First Name</label>
                        <input name="firstName" type="text" className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400">Last Name</label>
                        <input name="lastName" type="text" className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400">Email</label>
                        <input name="email" type="email" className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-400">Password</label>
                        <input name="password" type="password" className="w-full px-3 py-2 mt-1 text-white bg-gray-700 border border-gray-600 rounded-md" required />
                    </div>
                    <button type="submit" disabled={loading} className="w-full py-2 font-bold text-white bg-primary rounded-md hover:bg-blue-600 disabled:bg-gray-500">
                        {loading ? <Loader2 className="animate-spin mx-auto"/> : 'Register'}
                    </button>
                </form>
                <p className="text-sm text-center text-gray-400">
                    Already have an account? <a href="/login" className="font-medium text-primary hover:underline">Login</a>
                </p>
            </div>
        </div>
    );
};

export default Register;