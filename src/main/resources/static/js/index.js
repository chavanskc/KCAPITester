const envVariables = new Map();
const PLACEHOLDER_PATTERN = /\{\{([^{}]+)}}/g;
let selectedEnvKey = null;

function switchTab(tabId) {
    const tabs = document.querySelectorAll(".tab");
    const contents = document.querySelectorAll(".tab-content");

    tabs.forEach(tab => tab.classList.remove("active"));
    contents.forEach(content => content.classList.remove("active"));

    document.getElementById(tabId).classList.add("active");
    document.querySelector(`[data-tab="${tabId}"]`).classList.add("active");

    if (tabId === "tab1") {
        document.getElementById("inputType").value = "parameters";
    } else if (tabId === "tab2") {
        document.getElementById("inputType").value = "json";
    } else {
        document.getElementById("inputType").value = "env";
    }

    updateRequestControlsVisibility(tabId);
}

function updateRequestControlsVisibility(tabId) {
    const isEnvTab = tabId === "tab3";
    const methodSection = document.getElementById("methodSection");
    const baseUrlSection = document.getElementById("baseUrlSection");
    const submitSection = document.getElementById("submitSection");
    const baseUrlInput = document.getElementById("baseUrl");
    const methodInput = document.getElementById("method");

    methodSection.style.display = isEnvTab ? "none" : "block";
    baseUrlSection.style.display = isEnvTab ? "none" : "block";
    submitSection.style.display = isEnvTab ? "none" : "block";
    methodInput.required = !isEnvTab;
    baseUrlInput.required = !isEnvTab;
}

function addParameter() {
    const paramsDiv = document.getElementById("params");
    paramsDiv.appendChild(createParameterRow("", ""));
}

function createParameterRow(key, value) {
    const paramDiv = document.createElement("div");
    const keyInput = document.createElement("input");
    keyInput.type = "text";
    keyInput.name = "paramKeys";
    keyInput.placeholder = "Parameter Name";
    keyInput.value = key;
    keyInput.required = true;

    const valueInput = document.createElement("input");
    valueInput.type = "text";
    valueInput.name = "paramValues";
    valueInput.placeholder = "Parameter Value (supports {{KEY}})";
    valueInput.value = value;
    valueInput.required = true;

    const removeButton = document.createElement("button");
    removeButton.type = "button";
    removeButton.innerText = "Remove";
    removeButton.onclick = () => paramDiv.remove();

    paramDiv.appendChild(keyInput);
    paramDiv.appendChild(valueInput);
    paramDiv.appendChild(removeButton);
    return paramDiv;
}

function addEnvVariable() {
    resetEnvForm();
}

function setEnvFormMode(isEdit) {
    document.getElementById("envFormTitle").innerText = isEdit ? "Edit Environment Variable" : "Add Environment Variable";
    document.getElementById("envSaveButton").innerText = isEdit ? "Update" : "Add";
}

function resetEnvForm() {
    selectedEnvKey = null;
    document.getElementById("envKeyInput").value = "";
    document.getElementById("envValueInput").value = "";
    setEnvFormMode(false);
    renderEnvVariablesList();
}

function editEnvVariable(key) {
    if (!envVariables.has(key)) {
        return;
    }

    selectedEnvKey = key;
    document.getElementById("envKeyInput").value = key;
    document.getElementById("envValueInput").value = envVariables.get(key);
    setEnvFormMode(true);
    renderEnvVariablesList();
}

async function saveEnvVariable() {
    const keyInput = document.getElementById("envKeyInput").value.trim();
    const valueInput = document.getElementById("envValueInput").value;

    if (!keyInput) {
        alert("Environment key is required.");
        return;
    }

    if (keyInput.includes("#")) {
        alert("Environment key cannot contain #.");
        return;
    }

    if (!selectedEnvKey && envVariables.has(keyInput)) {
        alert("Key already exists. Please edit the existing key.");
        return;
    }

    if (selectedEnvKey && selectedEnvKey !== keyInput && envVariables.has(keyInput)) {
        alert("Cannot rename to an existing key.");
        return;
    }

    if (selectedEnvKey && selectedEnvKey !== keyInput) {
        envVariables.delete(selectedEnvKey);
    }

    envVariables.set(keyInput, valueInput);
    await syncEnvVariablesToServer();
    resetEnvForm();
}

async function removeEnvVariable(key) {
    if (!envVariables.has(key)) {
        return;
    }

    const confirmed = confirm("Remove environment variable: " + key + "?");
    if (!confirmed) {
        return;
    }

    envVariables.delete(key);
    if (selectedEnvKey === key) {
        selectedEnvKey = null;
        setEnvFormMode(false);
        document.getElementById("envKeyInput").value = "";
        document.getElementById("envValueInput").value = "";
    }

    await syncEnvVariablesToServer();
    renderEnvVariablesList();
}

function renderEnvVariablesList() {
    const envList = document.getElementById("envVarsList");
    envList.innerHTML = "";

    if (envVariables.size === 0) {
        const empty = document.createElement("div");
        empty.className = "env-empty";
        empty.innerText = "No environment variables added yet.";
        envList.appendChild(empty);
        return;
    }

    Array.from(envVariables.entries())
        .sort((a, b) => a[0].localeCompare(b[0]))
        .forEach(([key, value]) => {
            const row = document.createElement("div");
            row.className = "env-item" + (selectedEnvKey === key ? " selected" : "");

            const info = document.createElement("div");
            const keyDiv = document.createElement("div");
            keyDiv.className = "env-item-key";
            keyDiv.innerText = key;

            const valueDiv = document.createElement("div");
            valueDiv.className = "env-item-value";
            valueDiv.innerText = value;

            info.appendChild(keyDiv);
            info.appendChild(valueDiv);

            const actions = document.createElement("div");
            actions.className = "env-item-actions";

            const editButton = document.createElement("button");
            editButton.type = "button";
            editButton.className = "btn-edit";
            editButton.innerText = "Edit";
            editButton.onclick = () => editEnvVariable(key);

            const removeButton = document.createElement("button");
            removeButton.type = "button";
            removeButton.className = "btn-remove";
            removeButton.innerText = "Remove";
            removeButton.onclick = () => removeEnvVariable(key);

            actions.appendChild(editButton);
            actions.appendChild(removeButton);

            row.appendChild(info);
            row.appendChild(actions);
            envList.appendChild(row);
        });
}

async function loadEnvVariables() {
    try {
        const response = await fetch("/env-vars");
        if (!response.ok) {
            throw new Error("Unable to load env variables.");
        }

        const payload = await response.json();
        envVariables.clear();

        Object.entries(payload).forEach(([key, value]) => {
            envVariables.set(key, value);
        });
        resetEnvForm();
    } catch (error) {
        document.getElementById("error").innerText = "Unable to load environment variables.";
    }
}

async function syncEnvVariablesToServer() {
    const payload = Object.fromEntries(envVariables.entries());
    try {
        const response = await fetch("/env-vars", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errorBody = await response.json();
            throw new Error(errorBody.message || "Unable to save environment variables.");
        }
        document.getElementById("error").innerText = "";
    } catch (error) {
        document.getElementById("error").innerText = error.message;
    }
}

function resolvePlaceholders(text, missingKeys) {
    if (!text) {
        return text;
    }

    return text.replace(PLACEHOLDER_PATTERN, (match, key) => {
        const normalized = key.trim();
        if (!envVariables.has(normalized)) {
            missingKeys.add(normalized);
            return match;
        }
        return envVariables.get(normalized);
    });
}

function showMissingKeys(missingKeys) {
    const missingList = Array.from(missingKeys).join(", ");
    const message = "Missing environment variable(s): " + missingList;
    document.getElementById("error").innerText = message;
    alert(message);
}

function prepareForm(event) {
    event.preventDefault();
    document.getElementById("error").innerText = "";

    const inputType = document.getElementById("inputType").value;
    const method = document.getElementById("method").value;
    const baseUrl = document.getElementById("baseUrl").value;

    if (!baseUrl) {
        document.getElementById("error").innerText = "Base URL is required.";
        return;
    }

    if (inputType === "json") {
        handleJsonSubmission(baseUrl);
        return;
    }

    if (inputType === "parameters") {
        handleParameterSubmission(method, baseUrl);
    }
}

function handleJsonSubmission(baseUrl) {
    const missingKeys = new Set();
    const resolvedBaseUrl = resolvePlaceholders(baseUrl, missingKeys);
    const jsonInput = document.getElementById("jsonInput").value;

    if (!jsonInput || jsonInput.trim() === "") {
        document.getElementById("error").innerText = "JSON input is required.";
        return;
    }

    const resolvedJsonText = resolvePlaceholders(jsonInput, missingKeys);
    if (missingKeys.size > 0) {
        showMissingKeys(missingKeys);
        return;
    }

    let jsonPayload;
    try {
        jsonPayload = JSON.parse(resolvedJsonText);
    } catch (e) {
        document.getElementById("error").innerText = "Invalid JSON format: " + e.message;
        return;
    }

    jsonPayload.baseUrl = resolvedBaseUrl;

    fetch("/test-api", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(jsonPayload)
    })
        .then(response => response.text())
        .then(data => {
            document.getElementById("response").innerText = data;
            document.getElementById("error").innerText = "";
        })
        .catch(error => {
            document.getElementById("response").innerText = "";
            document.getElementById("error").innerText = "Error: " + error;
        });
}

function handleParameterSubmission(method, baseUrl) {
    const params = document.querySelectorAll("#params div");
    const missingKeys = new Set();
    const formData = new URLSearchParams();

    formData.append("baseUrl", resolvePlaceholders(baseUrl, missingKeys));

    params.forEach(param => {
        const keyInput = param.querySelector("input[name='paramKeys']");
        const valueInput = param.querySelector("input[name='paramValues']");
        if (!keyInput || !valueInput) {
            return;
        }

        const resolvedKey = resolvePlaceholders(keyInput.value, missingKeys);
        const resolvedValue = resolvePlaceholders(valueInput.value, missingKeys);
        if (resolvedKey && resolvedValue) {
            formData.append("paramKeys", resolvedKey);
            formData.append("paramValues", resolvedValue);
        }
    });

    if (missingKeys.size > 0) {
        showMissingKeys(missingKeys);
        return;
    }

    const requestOptions = method === "GET"
        ? {
            url: "/test-api?" + formData.toString(),
            options: { method: "GET" }
        }
        : {
            url: "/test-api",
            options: {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            }
        };

    fetch(requestOptions.url, requestOptions.options)
        .then(response => response.text())
        .then(data => {
            document.getElementById("response").innerText = data;
            document.getElementById("error").innerText = "";
        })
        .catch(error => {
            document.getElementById("response").innerText = "";
            document.getElementById("error").innerText = "Error: " + error;
        });
}

document.addEventListener("DOMContentLoaded", () => {
    loadEnvVariables();
    if (document.querySelectorAll("#params div").length === 0) {
        addParameter();
    }
    updateRequestControlsVisibility("tab1");
});

