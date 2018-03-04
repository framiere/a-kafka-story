package com.github.framiere;

public class Runner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length > 2) {
            System.err.println("required args: [producer|streamer] boostrapservers");
            System.err.println("ex: producer localhost:9092");
        }
        String type = args[0];
        switch (type) {
            case "producer":
                new RandomProducer().produce(args.length == 2 ? args[1] : "localhost:9092");
                break;
            case "streamer":
                new SimpleJoinStream().stream(args.length == 2 ? args[1] : "localhost:9092");
                break;
            default:
                throw new IllegalArgumentException(type + "is not supported");
        }
    }
}
