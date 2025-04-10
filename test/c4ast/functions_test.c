/* Тестовый файл для проверки определения и вызова функций */

// Прототипы функций
int sum(int a, int b);
int max(int a, int b);
int factorial(int n);
// void swap(int *a, int *b);

// Функция сложения
int sum(int a, int b) {
    return a + b;
}

// Функция нахождения максимума
int max(int a, int b) {
    if (a > b) {
        return a;
    } else {
        return b;
    }
}

// Рекурсивная функция для вычисления факториала
int factorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

// Функция для обмена значений через указатели
// void swap(int *a, int *b) {
//     int temp = *a;
//     *a = *b;
//     *b = temp;
// }

// Главная функция
int main() {
    int x = 5;
    int y = 10;
    int result = 0;
    
    // Вызовы функций
    result = sum(x, y);
    result = max(result, 20);
    result = factorial(4);
    
    // Использование указателей
    // swap(&x, &y);
    result = x + y;
    
    return result;
} 