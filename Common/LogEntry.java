package Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of {@code LogEntry} class used to represent
 * a entry on a log file
 */
public class LogEntry 
{
    /**
     * {@link LocalDateTime} repesenting the timestamp on the log
     */
    private LocalDateTime TimeStamp;
    /**
     * String containing the Entry on the log
     */
    private String Entry;

    /**
     * Class constructor
     * @param Type type
     * @param Address address
     * @param Entry entry
     */
    public LogEntry(String Entry)
    {
        this.Entry = Entry;
        this.TimeStamp = LocalDateTime.now();
    }

    /**
     * Getter for the entry
     * @return entry
     */
    public String getEntry()
    {
        return this.Entry;
    }

    /**
     * Getter for the timestamp
     * @return timestamp
     */
    public LocalDateTime getTimeStamp()
    {
        return this.TimeStamp;
    }

    /**
     * Setter for the timestamp
     * @param TimeStamp value to set
     */
    public void setTimeStamp(LocalDateTime TimeStamp)
    {
        this.TimeStamp = TimeStamp;
    }

    /**
     * Setter for the Entry
     * @param Entry value to set
     */
    public void setEntry(String Entry)
    {
        this.Entry = Entry;

    }
    
    @Override
    /**
     * String representation of the entry
     * @return String representation of the entry
     */
    public String toString()
    {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + this.TimeStamp.format(format) + "] : " + this.Entry + (this.Entry.endsWith("\n") ?"" :"\n");
    }
}