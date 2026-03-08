UPDATE user_preferences
SET appearance = 'light'
WHERE appearance IS NULL OR appearance NOT IN ('light', 'dark');
