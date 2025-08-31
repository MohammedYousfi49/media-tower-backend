import { createContext, useState, useEffect, useContext, ReactNode } from 'react';
import axiosClient from '../api/axiosClient';

interface Setting {
    key: string;
    value: string;
}

interface SettingsContextType {
    settings: Setting[];
    getSetting: (key: string) => string | undefined;
    loading: boolean;
}

const SettingsContext = createContext<SettingsContextType>({
    settings: [],
    getSetting: () => undefined,
    loading: true,
});

export const useSettings = () => {
    return useContext(SettingsContext);
};

export const SettingsProvider = ({ children }: { children: ReactNode }) => {
    const [settings, setSettings] = useState<Setting[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchSettings = async () => {
            try {
                const response = await axiosClient.get<Setting[]>('/settings');
                setSettings(response.data);
            } catch (error) {
                console.error('Failed to fetch settings:', error);
            } finally {
                setLoading(false);
            }
        };
        fetchSettings();
    }, []);

    const getSetting = (key: string): string | undefined => {
        const setting = settings.find(s => s.key === key);
        return setting?.value;
    };

    const value = { settings, getSetting, loading };

    return (
        <SettingsContext.Provider value={value}>
            {children}
        </SettingsContext.Provider>
    );
};