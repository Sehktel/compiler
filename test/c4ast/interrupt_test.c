/* Тестовый файл для проверки обработки прерываний */

// Объявление обработчика прерывания
void __attribute__((interrupt)) timer_interrupt_handler(void) {
    // Обработка прерывания таймера
    static int counter = 0;
    counter++;
    
    // Сброс флага прерывания
    // TIMER_FLAG = 0;
}

// Объявление обработчика прерывания с параметрами
void __attribute__((interrupt)) uart_interrupt_handler(int status) {
    // Обработка прерывания UART
    if (status & 0x01) {
        // Обработка приема данных
    }
    if (status & 0x02) {
        // Обработка передачи данных
    }
}

int main() {
    // Инициализация прерываний
    // ENABLE_INTERRUPTS();
    
    // Основной цикл программы
    while(1) {
        // Основной код программы
    }
    
    return 0;
} 