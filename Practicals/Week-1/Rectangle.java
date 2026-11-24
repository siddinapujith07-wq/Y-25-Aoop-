class Rectangle {
    double length, width;

    Rectangle(double l, double w) {
        length = l;
        width = w;
    }

    void calculate() {
        double area = length * width;
        double perimeter = 2 * (length + width);

        System.out.println("Area: " + area);
        System.out.println("Perimeter: " + perimeter);
    }

    public static void main(String[] args) {
        Rectangle r = new Rectangle(10, 5);
        r.calculate();
    }
}