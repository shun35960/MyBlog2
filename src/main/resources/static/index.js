$(window).on("load", function () {
    $("#createPasskey").on("click", () => createPasskey());
    $("#authenticatePasskey").on("click", () => signInWithPasskey());
});

 let optionsJSON;
    try {
        let optResp = await fetch("/webauthn/register/options", {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json",
                "X-CSRF-TOKEN": csrfToken
            }
        });
        optionsJSON = await optResp.json();
    } catch (e) {
        $("#statusCreatePasskey").text("Error: " + e);
        return;
    }