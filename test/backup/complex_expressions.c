/* Тестовый файл для проверки сложных выражений и операций */

int main() {
    int a = 5;
    int b = 10;
    int c = 15;
    int d = 20;
    int result = 0;
    
    // Сложные арифметические выражения с различными приоритетами
    result = a + b * c - d / a;
    result = (a + b) * (c - d) / (a + 1);
    result = ((a + b) * c) / (d - (a * b));
    
    // Логические операции
    if (a < b && c > d || a + b == c) {
        result = 1;
    } else if (!(a > b) && (c <= d || a != 0)) {
        result = 2;
    }
    
    // Битовые операции
    result = a & b | c ^ d;
    result = a << 2 | b >> 1;
    
    // Сложные присваивания
    result += a * b;
    result -= c / d;
    result *= a + b;
    result /= c - d;
    
    // Тернарный оператор
    // result = a > b ? c : d;
    // result = (a < b) ? (c > d ? a : b) : (c < d ? c : d);
    
    return result;
} 