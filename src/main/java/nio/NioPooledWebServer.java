package nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.List;

/**
 * Adaptation of the NIO Webserver which uses a thread pool to service channels.
 */
public class NioPooledWebServer extends NioWebServer
{
    private static final int MAX_THREADS = 5;
    private final ThreadPool threadPool = new ThreadPool (MAX_THREADS);

    public static void main (String[] argv) throws IOException
    {
        new NioPooledWebServer ().startServer (listenHost, listenPort, null);
    }

    /**
     * The handler method to process a channel with data ready to read. This handler delegates to a worker thread
     * in a thread pool to service the channel, then returns immediately.
     *
     * @param selectionKey A SelectionKey object representing a channel determined by the selector to be ready for
     *                     reading. If the channel returns an EOF condition, it is closed here, which automatically
     *                     invalidates the associated key. The selector will then de-register the channel on the next
     *                     select call.
     */
    protected void handleRead (SelectionKey selectionKey)
    {
        WorkerThread workerThread = threadPool.getWorker ();

        if (workerThread == null)
        {
            // No threads available. Do nothing. The selection loop will keep calling this method until a
            // thread becomes available. Can we improve this?
            return;
        }

        // Invoking this wakes up the worker thread, then returns
        workerThread.serviceChannel (selectionKey);
    }

    /**
     * A very simple thread pool class. The pool size is set at construction time and remains fixed. Threads are
     * cycled through a FIFO idle queue.
     */
    private class ThreadPool
    {
        final List <Thread> idleThreads = new LinkedList <> ();

        ThreadPool (int poolSize)
        {
            // Fill up the pool with worker threads
            for (int i = 0; i < poolSize; i++)
            {
                WorkerThread thread = new WorkerThread (this);

                // Set thread name for debugging. Start it.
                thread.setName ("Worker" + (i + 1));
                thread.start ();

                idleThreads.add (thread);
            }
        }

        /**
         * Find an idle worker thread, if any.  Could return null.
         */
        WorkerThread getWorker ()
        {
            WorkerThread workerThread = null;

            synchronized (idleThreads)
            {
                if (idleThreads.size () > 0)
                {
                    workerThread = (WorkerThread) idleThreads.remove (0);
                }
            }

            return (workerThread);
        }

        /**
         * Called by the worker thread to return itself to the idle pool.
         */
        void returnWorker (WorkerThread workerThread)
        {
            synchronized (idleThreads)
            {
                idleThreads.add (workerThread);
            }
        }
    }

    /**
     * A worker thread class which handles the HTTP protocol. Each instance is constructed with a reference to
     * the owning thread pool object. When started, the thread loops forever waiting to be awakened to service
     * the channel associated with a SelectionKey object. The worker is given a task by calling its serviceChannel()
     * method with a SelectionKey object. The serviceChannel() method stores the key reference in the thread object
     * then calls notify() to wake it up. Upon completion, the worker thread is returned to its parent thread-pool.
     */
    private class WorkerThread extends Thread
    {
        private final ThreadPool pool;
        private SelectionKey key;

        WorkerThread (ThreadPool pool)
        {
            this.pool = pool;
        }

        // Loop forever waiting for work to do
        public synchronized void run ()
        {
            System.out.println (this.getName () + " is ready");

            while (true)
            {
                try
                {
                    // Sleep and release object lock
                    this.wait ();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace ();
                    // Clear interrupt status
                    WorkerThread.interrupted ();
                }

                if (key == null)
                {
                    continue;    // just in case
                }

                System.out.println (this.getName () + " has been awakened");

                try
                {
                    drainChannel (key);
                }
                catch (Exception e)
                {
                    System.out.println ("Caught '" + e + "' closing channel");

                    // Close channel and nudge selector
                    try
                    {
                        key.channel ().close ();
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace ();
                    }

                    key.selector ().wakeup ();
                }

                key = null;

                // Done. Ready for more. Return to pool
                this.pool.returnWorker (this);
            }
        }

        /**
         * Called to initiate a unit of work by this worker thread on the provided SelectionKey object. This method
         * and the run() method are synchronized, so only one key can be serviced at a given time.
         * Before waking the worker thread, and before returning to the main selection loop, this key's interest set
         * is updated to remove OP_READ. This will cause the selector to ignore read-readiness for this channel while
         * the worker thread is servicing it.
         */
        synchronized void serviceChannel (SelectionKey key)
        {
            this.key = key;
            key.interestOps (key.interestOps () & (~SelectionKey.OP_READ));
            this.notify ();        // Awaken the thread
        }

        /**
         * This method drains the channel associated with the given key. This method assumes the key has been
         * modified prior to invocation to turn off selection interest in OP_READ.  When this method completes it
         * re-enables OP_READ and calls wakeup() on the selector so the selector will resume watching this channel.
         */
        void drainChannel (SelectionKey selectionKey) throws Exception
        {
            readChannelFully (selectionKey);
        }
    }
}
