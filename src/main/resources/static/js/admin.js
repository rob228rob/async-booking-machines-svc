let pollingInterval = 150; // Интервал в миллисекундах
let maxAttempts = 5; // Максимальное количество попыток

document.addEventListener('DOMContentLoaded', async () => {
    await fetchDormitories();
    await fetchMachines();
    // можно включить сразу загрузку логов, можно не включать
    //await fetchLogs(0);

    // Обработчик отправки формы машинки
    const addMachineForm = document.getElementById('add-machine-form');
    addMachineForm.addEventListener('submit', handleAddMachine);

    // Обработчик отправки формы для общежитий
    const addDormitoryForm = document.getElementById('add-dormitory-form');
    addDormitoryForm.addEventListener('submit', handleAddDormitory);

    // Обработчик кнопки загрузки логов
    const loadLogsButton = document.getElementById('load-logs-button');
    loadLogsButton.addEventListener('click', () => {
        const limitInput = document.getElementById('log-limit');
        const limit = parseInt(limitInput.value);
        if (isNaN(limit) || limit < 1) {
            showErrorMessage('Пожалуйста, введите корректное число для лимита логов.');
            return;
        }
        fetchLogs(limit);
    });
});

/**
 * Функция для получения всех общежитий и заполнения выпадающего списка
 */
async function fetchDormitories() {
    try {
        const response = await fetch('/api/location/get-all', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok || response.status === 202) {
            const dormitories = await response.json();
            populateDormitorySelect(dormitories);
            populateDormitoriesTable(dormitories);
        } else {
            console.error('Не удалось получить список общежитий.');
            showErrorMessage('Не удалось загрузить список общежитий.');
        }
    } catch (error) {
        console.error('Ошибка при получении общежитий:', error);
        showErrorMessage('Произошла ошибка при загрузке общежитий.');
    }
}

/**
 * Функция для заполнения выпадающего списка общежитий
 * @param {Array} dormitories - Массив общежитий
 */
function populateDormitorySelect(dormitories) {
    const dormitorySelect = document.getElementById('dormitory');

    dormitories.forEach(dorm => {
        const option = document.createElement('option');
        option.value = dorm.id;
        option.textContent = dorm.name;
        dormitorySelect.appendChild(option);
    });
}

/**
 * Функция для заполнения таблицы общежитий
 * @param {Array} dormitories - Массив общежитий
 */
function populateDormitoriesTable(dormitories) {
    const dormitoriesTableBody = document.querySelector('#dormitories-table tbody');
    dormitoriesTableBody.innerHTML = ''; // Очистка таблицы

    dormitories.forEach(dorm => {
        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${dorm.id}</td>
            <td>${dorm.name}</td>
            <td>${dorm.dormitoryAddress || 'Не указано'}</td>
            <td>
                <button class="delete-button" data-dorm-id="${dorm.id}">Удалить</button>
            </td>
        `;

        dormitoriesTableBody.appendChild(row);
    });

    // Добавляем обработчики событий для кнопок удаления общежитий
    document.querySelectorAll('.delete-button').forEach(button => {
        button.addEventListener('click', handleDeleteDormitory);
    });
}

/**
 * Функция для получения всех машинок и заполнения таблицы машинок
 */
async function fetchMachines() {
    try {
        const response = await fetch('/api/machines/get-all', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            const machines = await response.json();
            populateMachinesTable(machines);
        } else {
            console.error('Не удалось получить список машинок.');
            showErrorMessage('Не удалось загрузить список машинок.');
        }
    } catch (error) {
        console.error('Ошибка при получении машинок:', error);
        showErrorMessage('Произошла ошибка при загрузке машинок.');
    }
}

/**
 * Функция для заполнения таблицы машинок
 * @param {Array} machines - Массив машинок
 */
function populateMachinesTable(machines) {
    const machinesTableBody = document.querySelector('#machines-table tbody');
    machinesTableBody.innerHTML = ''; // Очистка таблицы

    machines.forEach(machine => {
        const row = document.createElement('tr');

        // содержит поля: id, name, type, dormitoryName
        row.innerHTML = `
            <td>${machine.id}</td>
            <td>${machine.name}</td>
            <td>${machine.type === 1 ? 'Стиральная Машинка (WASHER)' : 'Сушильная Машинка (DRYER)'}</td>
            <td>${machine.dormitoryName || 'Не указано'}</td>
            <td>
                <button class="delete-button" data-machine-id="${machine.id}">Удалить</button>
            </td>
        `;

        machinesTableBody.appendChild(row);
    });

    // Добавляем обработчики событий для кнопок удаления машинок
    document.querySelectorAll('.delete-button').forEach(button => {
        button.addEventListener('click', handleDeleteMachine);
    });
}


/**
 * Функция для получения и отображения логов
 * @param {number} limit - Количество последних логов для загрузки
 */
async function fetchLogs(limit) {

    try {
        const response = await fetch(`/api/reservations/get-logs?limit=${limit}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            const logs = await response.json();
            populateLogsTable(logs);
        } else {
            console.error('Не удалось получить список логов.');
            showErrorMessage('Не удалось загрузить список логов.');
        }
    } catch (error) {
        console.error('Ошибка при получении логов:', error);
        showErrorMessage('Произошла ошибка при загрузке логов.');
    }
}

/**
 * Обработчик отправки формы для добавления нового общежития
 * @param {Event} event - Событие отправки формы
 */
async function handleAddDormitory(event) {
    event.preventDefault(); // Предотвращаем стандартное поведение формы

    const dormName = document.getElementById('dorm-name').value.trim();
    const dormAddress = document.getElementById('dorm-address').value.trim();

    // Валидация данных
    if (!dormName || !dormAddress) {
        showErrorMessage('Пожалуйста, заполните все поля.');
        return;
    }

    const dormitoryRequest = {
        name: dormName,
        address: dormAddress
    };

    try {
        const response = await fetch('/api/location/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            credentials: 'include',
            body: new URLSearchParams(dormitoryRequest)
        });

        if (response.status === 202) { // HTTP 202 Accepted
            showSuccessMessage('Запрос на добавление получен в работу');
            // Очистка формы
            event.target.reset();
            const statusUrl = response.headers.get('Location');
            const id = extractReservationIdFromUrl(statusUrl);
            pollingAsyncOperationStatus(id, statusUrl, fetchDormitories);
        } else {
            const errorData = await response.text();
            console.error('Ошибка при добавлении общежития:', errorData);
            showErrorMessage('Не удалось добавить общежитие. Проверьте данные и попробуйте снова.');
        }
    } catch (error) {
        console.error('Ошибка при добавлении общежития:', error);
        showErrorMessage('Произошла ошибка при добавлении общежития.');
    }
}


/**
 * Функция для извлечения reservationId из URL статуса
 * @param {string} statusUrl - URL статуса
 * @returns {string} - reservationId
 */
function extractReservationIdFromUrl(statusUrl) {
    const parts = statusUrl.split('/');
    return parts[parts.length - 1];
}

/**
 * Функция для поллинга статуса бронирования с использованием SweetAlert2
 * @param {string} operationId - ID бронирования
 * @param {string} statusUrl - URL для проверки статуса
 * @param call_back
 */
function pollingAsyncOperationStatus(operationId, statusUrl, call_back) {

    let attempts = 0;

    console.log(`Начало поллинга статуса бронирования ID: ${operationId}`);

    const intervalId = setInterval(async () => {
        console.log(`Проверка статуса бронирования ID: ${operationId}, попытка ${attempts + 1}`);

        try {
            const response = await fetch(statusUrl, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            if (response.ok) {
                const statusText = await response.text();
                console.log(`Статус бронирования ID: ${operationId} - ${statusText}`);

                if (statusText === 'Operation completed successfully') {
                    console.log({
                        title: 'завершено',
                        icon: 'success',
                        timer: 5000,
                        showConfirmButton: false
                    });
                    await call_back();
                    clearInterval(intervalId);

                } else if (statusText.startsWith('Operation failed')) {
                    console.log({
                        title: 'Ошибка',
                        text: `Сохранение не удалось: ${statusText}`,
                        icon: 'error',
                        timer: 5000,
                        showConfirmButton: false
                    });
                    alert(`Сохранение не удалось: ${statusText}`)
                    clearInterval(intervalId);
                } else {
                    console.log(`Текущее состояние (${operationId}): ${statusText}`);
                }
            } else if (response.status === 404) {
                console.error('Статус не найден.');
                console.log({
                    title: 'Ошибка',
                    icon: 'error',
                    confirmButtonText: 'Ок'
                });
                alert(`Статус бронирования не найден`)
                clearInterval(intervalId);
            } else {
                const errorData = await response.json();
                console.error('Ошибка при проверке статуса:', errorData);
                console.log({
                    title: 'Ошибка',
                    text: 'Не удалось проверить статус.',
                    icon: 'error',
                    confirmButtonText: 'Ок'
                });
                alert(`не удалось проверить статус: ${errorData}`);
                clearInterval(intervalId);
            }
        } catch (error) {
            console.error('Ошибка при выполнении операции, завершилась неуспешно');
            console.log({
                title: 'Ошибка',
                text: 'Произошла ошибка при проверке статуса.',
                icon: 'error',
                confirmButtonText: 'Ок'
            });
            alert(`Ошибка при выполнении операции, она завершилась неуспешно`);
            clearInterval(intervalId);
        }

        attempts++;
        if (attempts >= maxAttempts) {
            console.error('Время ожидания ответа от сервера истекло.');
            console.log({
                title: 'Время истекло',
                text: 'Время ожидания ответа от сервера истекло.',
                icon: 'warning',
                confirmButtonText: 'Ок'
            });
            clearInterval(intervalId);
        }
    }, pollingInterval);
}

/**
 * Функция для заполнения таблицы логов
 * @param {Array} logs - Массив логов
 */
function populateLogsTable(logs) {
    const logsTableBody = document.querySelector('#logs-table tbody');
    logsTableBody.innerHTML = ''; // Очистка таблицы

    if (logs.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="5" style="text-align: center;">Нет доступных логов.</td>
        `;
        logsTableBody.appendChild(row);
        return;
    }

    logs.forEach(log => {
        const row = document.createElement('tr');

        row.innerHTML = `
            <td>${log.action}</td>
            <td>${log.reservationId}</td>
            <td>${new Date(log.timestamp).toLocaleString()}</td>
            <td>${log.oldData ? JSON.stringify(JSON.parse(log.oldData), null, 2) : 'N/A'}</td>
            <td>${log.newData ? JSON.stringify(JSON.parse(log.newData), null, 2) : 'N/A'}</td>
        `;

        logsTableBody.appendChild(row);
    });
}


/**
 * Обработчик отправки формы для добавления новой машинки
 * @param {Event} event - Событие отправки формы
 */
async function handleAddMachine(event) {
    event.preventDefault(); // Предотвращаем стандартное поведение формы

    const dormitoryId = document.getElementById('dormitory').value;
    const machineName = document.getElementById('machine-name').value.trim();
    const machineType = parseInt(document.getElementById('machine-type').value);

    // Валидация данных
    if (!dormitoryId || !machineName || !machineType) {
        showErrorMessage('Пожалуйста, заполните все поля.');
        return;
    }

    const machineRequest = {
        dormitoryId: dormitoryId,
        name: machineName,
        type: machineType
    };

    try {
        const response = await fetch('/api/machines/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(machineRequest)
        });

        if (response.status === 202) { // HTTP 202 Accepted
            showSuccessMessage('Машинка успешно добавлена!');
            // Очистка формы
            event.target.reset();
            const statusUrl = response.headers.get('Location');
            const id = extractReservationIdFromUrl(statusUrl);
            showSuccessMessage('Операция инициирована. Ожидайте обновления статуса.');

            pollingAsyncOperationStatus(id, statusUrl, fetchMachines);
        } else {
            const errorData = await response.json();
            console.error('Ошибка при добавлении машинки:', errorData);
            showErrorMessage('Не удалось добавить машинку. Проверьте данные и попробуйте снова.');
        }
    } catch (error) {
        console.error('Ошибка при добавлении машинки:', error);
        showErrorMessage('Произошла ошибка при добавлении машинки.');
    }
}

/**
 * Обработчик удаления общежития
 * @param {Event} event - Событие клика на кнопке удаления
 */
async function handleDeleteDormitory(event) {
    const locationId = event.target.getAttribute('data-dorm-id');
    if (!locationId) return;

    // Подтверждение действия
    const confirmDelete = confirm('Вы уверены, что хотите удалить это общежитие? Это действие нельзя отменить.');
    if (!confirmDelete) return;

    try {
        const response = await fetch(`/api/location/del/${locationId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            showSuccessMessage('Общежитие успешно удалено!');
            // Обновление
            await fetchDormitories();
            await fetchMachines();
        } else {
            const errorData = await response.json();
            console.error('Ошибка при удалении общежития:', errorData);
            showErrorMessage('Не удалось удалить общежитие. Попробуйте снова.');
        }
    } catch (error) {
        console.error('Ошибка при удалении общежития:', error);
        showErrorMessage('Произошла ошибка при удалении общежития.');
    }
}

/**
 * Обработчик удаления машинки
 * @param {Event} event - Событие клика на кнопке удаления
 */
async function handleDeleteMachine(event) {
    const machineId = event.target.getAttribute('data-machine-id');
    if (!machineId) return;

    // Подтверждение действия
    const confirmDelete = confirm('Вы уверены, что хотите удалить эту машинку? Это действие нельзя отменить.');
    if (!confirmDelete) return;

    try {
        const response = await fetch(`/api/machines/del/${machineId}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        });

        if (response.ok) {
            showSuccessMessage('Машинка успешно удалена!');
            // Обновлени
            await fetchMachines();
        } else {
            const errorData = await response.json();
            console.error('Ошибка при удалении машинки:', errorData);
            showErrorMessage('Не удалось удалить машинку. Попробуйте снова.');
        }
    } catch (error) {
        console.error('Ошибка при удалении машинки:', error);
        showErrorMessage('Произошла ошибка при удалении машинки.');
    }
}

/**
 * Функция для отображения сообщения об успешном действии
 * @param {string} message - Текст сообщения
 */
function showSuccessMessage(message) {
    const successMessage = document.getElementById('success-message');
    successMessage.textContent = message;
    successMessage.classList.remove('error');
    successMessage.classList.add('success');
    successMessage.style.display = 'block';

    // Скрыть сообщение через 5 секунд
    setTimeout(() => {
        successMessage.style.display = 'none';
    }, 5000);
}

/**
 * Функция для отображения сообщения об ошибке
 * @param {string} message - Текст сообщения
 */
function showErrorMessage(message) {
    const errorMessage = document.getElementById('error-message');
    errorMessage.textContent = message;
    errorMessage.classList.remove('success');
    errorMessage.classList.add('error');
    errorMessage.style.display = 'block';

    setTimeout(() => {
        errorMessage.style.display = 'none';
    }, 3000);
}
