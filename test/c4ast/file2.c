// Условные операторы и циклы
int main() {
    int x = 10;
    int y = 0;
    
    if (x > 0) {
        while (x > 0) {
            y = y + 1;
            x = x - 1;
        }
    } else {
        y = -1;
    }
    
    return y;
} 