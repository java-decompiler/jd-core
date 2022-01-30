package org.jd.core.v1.stub;

public class SwitchEnum {
    
    public enum ColourEnum {
        RED, GREEN, BLUE
    }
    
    public enum FruitEnum {
        BANANA, APPLE, KIWI
    }
    
    static class ColourObject {
        public ColourEnum getType() {
            return ColourEnum.BLUE;
        }
    }
    
    static class FruitObject {
        public FruitEnum getType() {
            return FruitEnum.KIWI;
        }
    }
    
    public static void main(String[] args) {
        ColourObject colourObject = new ColourObject();
        print(colourObject);
        FruitObject fruitObject = new FruitObject();
        print(fruitObject);
    }

    private static void print(FruitObject fruitObject) {
        switch (fruitObject.getType()) {
        case APPLE:
            System.out.println("Apple");
            break;
        case BANANA:
            System.out.println("Banana");
            break;
        case KIWI:
            System.out.println("Kiwi");
            break;
        default:
            System.out.println("Default (fruit)");
            break;
        }

    }

    private static void print(ColourObject colourObject) {
        switch (colourObject.getType()) {
        case GREEN:
            System.out.println("Green");
            break;
        case BLUE:
            System.out.println("Blue");
            break;
        case RED:
            System.out.println("Red");
            break;
        default:
            System.out.println("Default (colour)");
            break;
        }
    }
}