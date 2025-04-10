/* Тестовый файл для проверки использования заголовочного файла */

#include "custom_header.h"

// Реализация функций из заголовочного файла
int add(char a, char b) {
    return a + b;
}

int subtract(char a, char b) {
    return a - b;
}

int product(char a, char b) {
    return a * b;
}
void main() {
    
    // Использование функций
    int sum = add(P1, P3);
    int diff = subtract(P1, P3);
    int product = multiply(P1, P3);
    
} 