class Book {
    int ISBN;
    String title;
    double price;

    Book(int i, String t, double p) {
        ISBN = i;
        title = t;
        price = p;
    }

    void display() {
        System.out.println("ISBN: " + ISBN);
        System.out.println("Title: " + title);
        System.out.println("Price: " + price);
    }

    public static void main(String[] args) {
        Book b = new Book(12345, "Java Programming", 450);
        b.display();
    }
}