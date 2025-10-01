// Dark mode functionality - runs immediately
document.addEventListener('DOMContentLoaded', function() {
    // Check for saved theme preference or default to light mode
    const currentTheme = localStorage.getItem('theme') || 'light';

    if (currentTheme === 'dark') {
        document.body.classList.add('dark-mode');
    }

    // Dark mode toggle button
    const darkModeToggle = document.getElementById('darkModeToggle');
    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function() {
            console.log('Toggle clicked!'); // デバッグ用
            document.body.classList.toggle('dark-mode');

            // Save preference
            if (document.body.classList.contains('dark-mode')) {
                localStorage.setItem('theme', 'dark');
                console.log('Dark mode ON');
            } else {
                localStorage.setItem('theme', 'light');
                console.log('Light mode ON');
            }
        });
    } else {
        console.error('darkModeToggle button not found!');
    }

    // Passkey functionality (jQuery)
    if (typeof $ !== 'undefined') {
        $("#createPasskey").on("click", () => createPasskey());
        $("#authenticatePasskey").on("click", () => signInWithPasskey());
    }
});
