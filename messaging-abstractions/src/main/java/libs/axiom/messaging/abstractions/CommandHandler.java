package libs.axiom.messaging.abstractions;

public interface CommandHandler<T extends Command> extends MessageHandler<T> {
}
