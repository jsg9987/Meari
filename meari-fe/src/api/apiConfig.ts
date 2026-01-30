/**
 * API Mocking Configuration Utility
 * Determines whether to use mock functions or real API calls based on environment variables.
 */

const getEnvBool = (key: string, defaultValue: boolean = false): boolean => {
    const value = import.meta.env[key];
    if (value === undefined) return defaultValue;
    return value === 'true';
};

// Global override: If this is true, ALL APIs will use mocks regardless of specific flags.
const GLOBAL_MOCK = getEnvBool('VITE_USE_MOCK_API', false);

export const apiConfig = {
    /**
     * Checks if mocking is enabled for a specific feature.
     * @param feature The name of the feature (e.g., 'AUTH', 'ROOMS', 'WEBRTC')
     * @returns true if mocking is enabled
     */
    shouldMock: (feature: 'AUTH' | 'ROOMS' | 'WEBRTC'): boolean => {
        if (GLOBAL_MOCK) return true;

        const featureFlag = `VITE_USE_MOCK_${feature}`;
        return getEnvBool(featureFlag, false);
    }
};
