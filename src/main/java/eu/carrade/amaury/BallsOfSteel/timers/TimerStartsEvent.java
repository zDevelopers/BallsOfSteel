package eu.carrade.amaury.BallsOfSteel.timers;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


/**
 * This event is fired when a timer ends.
 *
 * @author Amaury Carrade
 */
public final class TimerStartsEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    private Timer timer;

    public TimerStartsEvent(Timer timer)
    {
        this.timer = timer;
    }

    /**
     * Returns the timer.
     */
    public Timer getTimer()
    {
        return timer;
    }

    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}
