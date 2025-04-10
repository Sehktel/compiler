/* Тестовый файл для проверки битовых операций */


// Функция проверки бита
unsigned char check_bit(unsigned char reg, unsigned char bit) {
    return (reg & (1 << bit)) != 0;
}

// Функция для работы с битовыми полями
unsigned char extract_bits(unsigned char value, unsigned char start, unsigned char length) {
    return (value >> start) & ((1 << length) - 1);
}

// Главная функция
int main() {
    unsigned char port = 0x00;  // Исходное значение порта
    
    // Установка отдельных битов
    set_bit(&port, 0);  // Устанавливаем бит 0 -> 0x01
    set_bit(&port, 3);  // Устанавливаем бит 3 -> 0x09
    
    // Переключение бита
    toggle_bit(&port, 0);  // Переключаем бит 0 -> 0x08
    toggle_bit(&port, 1);  // Переключаем бит 1 -> 0x0A
    
    // Проверка битов
    unsigned char bit0 = check_bit(port, 0);  // Должно быть 0
    unsigned char bit1 = check_bit(port, 1);  // Должно быть 1
    unsigned char bit3 = check_bit(port, 3);  // Должно быть 1
    
    // Сброс бита
    clear_bit(&port, 3);  // Сбрасываем бит 3 -> 0x02
    
    // Составное значение
    unsigned char value = 0x5A;  // 01011010 в двоичном виде
    
    // Извлечение битовых полей
    unsigned char low_nibble = extract_bits(value, 0, 4);   // Должно быть 0x0A
    unsigned char high_nibble = extract_bits(value, 4, 4);  // Должно быть 0x05
    
    // Битовые операции с масками
    unsigned char mask1 = 0x0F;  // Маска для младших 4 битов
    unsigned char mask2 = 0xF0;  // Маска для старших 4 битов
    
    unsigned char result1 = value & mask1;  // Должно быть 0x0A
    unsigned char result2 = value & mask2;  // Должно быть 0x50
    
    // Сдвиги
    unsigned char left_shift = value << 1;   // Должно быть 0xB4
    unsigned char right_shift = value >> 1;  // Должно быть 0x2D
    
    return 0;
} 