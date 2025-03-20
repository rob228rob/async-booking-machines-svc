
let selectedMachineId = null;
const pollingInterval = 150; // Интервал в мс
const maxAttempts = 5;       // Макс. кол-во запросов при поллинге

document.addEventListener('DOMContentLoaded', () => {
loadMachines().then(() => console.log("Machines loaded"));
});

/**
* 1) Получить список коворков с бекенда (/api/machines/get-all),
*    затем вызвать displayMachinesList(...)
*/
async function loadMachines() {
try {
const response = await fetch('/api/machines/get-all', {
method: 'GET',
credentials: 'include'
});
if (response.ok) {
const data = await response.json();
displayMachinesList(data);
} else {
console.error('Не удалось получить список коворков.');
showErrorMessage('Ошибка при загрузке списка коворков.');
}
} catch (error) {
console.error('Ошибка при получении списка коворков:', error);
showErrorMessage('Произошла ошибка при загрузке коворков.');
}
}

/**
* 2) Отображаем список коворков на странице.
*    Предполагаем, что "data" – это массив коворков, каждый объект вида:
*    { id, name, locationName, machineType, ... }
*/
function displayMachinesList(machines) {
const machinesList = document.getElementById('machines-list');
if (!machinesList) {
console.warn('Не найден элемент #machines-list в HTML.');
return;
}
machinesList.innerHTML = '';

machines.forEach(machine => {
const machineCard = document.createElement('div');
machineCard.classList.add('machine-card');
machineCard.innerHTML = `
        <h3>${machine.name}</h3>
        <p><strong>Локация:</strong> ${machine.locationName || 'Неизвестно'}</p>
        <p><strong>Тип машинки:</strong> ${machine.machineType || 'Неизвестно'}</p>
    `;
// При клике выбираем машинку и показываем форму выбора слотов
machineCard.addEventListener('click', () => {
selectedMachineId = machine.id;
document.getElementById('slots-controls').style.display = 'flex';
document.getElementById('machines-container').innerHTML = '';
});

machinesList.appendChild(machineCard);
});

// Кнопка "Загрузить слоты"
const loadSlotsButton = document.getElementById('load-slots-button');
if (loadSlotsButton) {
loadSlotsButton.addEventListener('click', loadMachineTimeSlots);
}
}

/**
* 3) Загрузить слоты для выбранной машинки, начиная со startDate, на указанное кол-во недель
*/
async function loadMachineTimeSlots() {
if (!selectedMachineId) {
alert('Сначала выберите машинку');
return;
}

const startDateInput = document.getElementById('start-date');
const weeksSelect = document.getElementById('weeks');

if (!startDateInput || !weeksSelect) {
console.error('Не найдены поля #start-date или #weeks в HTML.');
return;
}

const startDate = startDateInput.value;
const weeks = parseInt(weeksSelect.value);

if (!startDate) {
alert('Пожалуйста, выберите начальную дату');
return;
}
if (isNaN(weeks) || weeks < 1) {
alert('Некорректное количество недель');
return;
}

// Сохраняем дату и weeks для дальнейшего расчёта
loadMachineTimeSlots.startDate = new Date(startDate);
loadMachineTimeSlots.weeks = weeks;

const url = `/api/machine-slots/get?machineId=${selectedMachineId}&startDate=${startDate}&weeks=${weeks}`;
try {
const response = await fetch(url, { method: 'GET', credentials: 'include' });
if (response.ok) {
// Бэкенд возвращает объект вида { slots: [ {machineId, machineName, locationName, timeSlots: [...]}, ... ] }
const data = await response.json();
displayGeneratedSlots(data.slots);
} else {
console.error('Не удалось получить слоты для машинки.', response.status);
showErrorMessage('Ошибка при загрузке слотов.');
}
} catch (error) {
console.error('Ошибка при получении слотов:', error);
showErrorMessage('Произошла ошибка при загрузке слотов.');
}
}

/**
* 4) Отобразить слоты.
*    "machines" → массив, где каждый элемент – объект { machineId, machineName, locationName, timeSlots: [...] }.
*/
function displayGeneratedSlots(machines) {
const machinesContainer = document.getElementById('machines-container');
if (!machinesContainer) {
console.warn('Не найден элемент #machines-container.');
return;
}
machinesContainer.innerHTML = '';

const { startDate, weeks } = loadMachineTimeSlots;

// Генерируем все "даты" на фронте, чтобы рассчитать weekNumber.
const allDates = [];
const start = new Date(startDate);
// Ставим Monday как начало
start.setDate(start.getDate() - ((start.getDay() + 6) % 7));

for (let week = 0; week < weeks; week++) {
for (let day = 1; day <= 7; day++) {
const date = new Date(start);
date.setDate(start.getDate() + week * 7 + (day - 1));
allDates.push({
dayOfWeek: day,
date: date.toISOString().split('T')[0]
});
}
}

// Создаём map { "dayOfWeek-weekNumber": "YYYY-MM-DD" }
const dateMap = {};
allDates.forEach((item, index) => {
const weekNumber = Math.floor(index / 7) + 1;
dateMap[`${item.dayOfWeek}-${weekNumber}`] = item.date;
});

// Проходимся по каждому объекту: { machineId, machineName, timeSlots: [...] }
machines.forEach((machine) => {
machine.timeSlots.forEach((slot, index) => {
// Если в бэке нет чёткого порядка, эта логика "weekNumber" может быть упрощена.
// Но оставим, как в исходном коде.
const weekNumber = Math.floor((index / machine.timeSlots.length) * weeks) + 1;
const resDate = dateMap[`${slot.dayOfWeek}-${weekNumber}`] || '';

if (!resDate) {
console.error(`Не удалось определить дату для dayOfWeek=${slot.dayOfWeek}, weekNumber=${weekNumber}`);
return;
}

// Ищем div для этой «машинки». Если нет — создаём.
let machineCard = machinesContainer.querySelector(`[data-machine-id="${machine.machineId}"]`);
if (!machineCard) {
machineCard = document.createElement('div');
machineCard.classList.add('machine-card');
machineCard.setAttribute('data-machine-id', machine.machineId);
machineCard.innerHTML = `
                <h3>${machine.machineName}</h3>
                <p><strong>Локация:</strong> ${machine.locationName || 'Неизвестно'}</p>
                <ul class="time-slots"></ul>
            `;
machinesContainer.appendChild(machineCard);
}

const timeSlotsList = machineCard.querySelector('.time-slots');

// Проверяем поле "available" (или "isAvailable") в JSON
const isAvailable = slot.available; // Или slot.isAvailable
const timeSlotItem = document.createElement('li');
timeSlotItem.classList.add(
'time-slot',
isAvailable ? 'available' : 'booked'
);

timeSlotItem.innerHTML = `
            <strong>${getDayOfWeekName(slot.dayOfWeek)} (${resDate})</strong>:
            <span>${slot.startTime} - ${slot.endTime}</span>
            ${
isAvailable
? `<button class="book"
                          data-machine-id="${machine.machineId}"
                          data-res-date="${resDate}"
                          data-start-time="${slot.startTime}"
                          data-end-time="${slot.endTime}">
                     Забронировать
                   </button>`
: `<button class="booked" disabled>Забронировано</button>`
}
        `;

timeSlotsList.appendChild(timeSlotItem);
});
});

// Вешаем обработчик клика "Забронировать"
document.querySelectorAll('.book').forEach(button => {
button.addEventListener('click', handleBooking);
});
}

/**
* 5) Отправка бронирования слота на /api/reservations/book (POST JSON)
*/
async function handleBooking(event) {
const machineId = event.target.getAttribute('data-machine-id');
const resDate = event.target.getAttribute('data-res-date');
const startTime = event.target.getAttribute('data-start-time');
const endTime = event.target.getAttribute('data-end-time');

if (!machineId || !resDate || !startTime || !endTime) {
console.error('Недостаточно данных для бронирования.');
showErrorMessage('Отсутствуют необходимые данные для бронирования.');
return;
}

const reservationRequest = {
machineId,
resDate,
startTime,
endTime
};

try {
const response = await fetch('/api/reservations/book', {
method: 'POST',
headers: { 'Content-Type': 'application/json' },
credentials: 'include',
body: JSON.stringify(reservationRequest)
});

if (response.status === 202) {
const statusUrl = response.headers.get('Location');
const reservationId = extractReservationIdFromUrl(statusUrl);
showSuccessMessage('Бронирование инициировано. Ожидайте обновления статуса.');

pollReservationStatus(reservationId, statusUrl);
} else if (response.status === 400) {
alert('Невалидная дата (например, в прошлом).');
} else {
const errorData = await response.json();
alert(`Ошибка при бронировании: ${errorData.message || 'Неизвестная ошибка.'}`);
}
} catch (error) {
console.error('Ошибка при бронировании:', error);
alert('Произошла ошибка при бронировании. Повторите позже.');
}
}

/**
* Извлекаем reservationId из URL типа "/api/reservations/status/abcdef-12345"
*/
function extractReservationIdFromUrl(statusUrl) {
const parts = statusUrl.split('/');
return parts[parts.length - 1];
}

/**
* 6) Поллинг статуса бронирования, пока оно не завершится успешно/неудачно
*/
function pollReservationStatus(reservationId, statusUrl) {
let attempts = 0;

const intervalId = setInterval(async () => {
try {
const response = await fetch(statusUrl, {
method: 'GET',
headers: { 'Content-Type': 'application/json' },
credentials: 'include'
});

if (response.ok) {
const statusText = await response.text();
if (statusText === 'Operation completed successfully') {
clearInterval(intervalId);
showSuccessMessage('Бронирование успешно!');
// Обновить слоты
await loadMachineTimeSlots();
} else if (statusText.startsWith('Operation failed')) {
clearInterval(intervalId);
alert(`Бронирование не удалось: ${statusText}`);
} else {
console.log(`Текущее состояние [${reservationId}]: ${statusText}`);
}
} else if (response.status === 404) {
console.error('Статус бронирования не найден');
clearInterval(intervalId);
} else {
console.error('Не удалось проверить статус бронирования');
clearInterval(intervalId);
}
} catch (error) {
console.error('Ошибка при проверке статуса бронирования:', error);
clearInterval(intervalId);
}

attempts++;
if (attempts >= maxAttempts) {
console.error('Время ожидания ответа от сервера истекло.');
clearInterval(intervalId);
}
}, pollingInterval);
}

/**
* Функция для отображения сообщения об успехе
*/
function showSuccessMessage(message) {
let successMessage = document.getElementById('success-message');
if (!successMessage) {
// На случай, если нет такого элемента, можем его создать
successMessage = document.createElement('div');
successMessage.id = 'success-message';
document.body.appendChild(successMessage);
}
successMessage.textContent = message;
successMessage.classList.remove('error');
successMessage.classList.add('success');
successMessage.style.display = 'block';

setTimeout(() => {
successMessage.style.display = 'none';
}, 5000);
}

/**
* Функция для отображения сообщения об ошибке
*/
function showErrorMessage(message) {
let errorMessage = document.getElementById('error-message');
if (!errorMessage) {
// Если не нашли элемент, создадим
errorMessage = document.createElement('div');
errorMessage.id = 'error-message';
document.body.appendChild(errorMessage);
}
errorMessage.textContent = message;
errorMessage.classList.remove('success');
errorMessage.classList.add('error');
errorMessage.style.display = 'block';

setTimeout(() => {
errorMessage.style.display = 'none';
}, 5000);
}

/**
* Утильные методы
*/
function getDayOfWeekName(dayOfWeek) {
const days = ['Понедельник','Вторник','Среда','Четверг','Пятница','Суббота','Воскресенье'];
return days[dayOfWeek - 1] || 'Неизвестный день';
}

