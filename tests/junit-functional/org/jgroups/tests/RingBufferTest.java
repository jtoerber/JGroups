package org.jgroups.tests;

import org.jgroups.Global;
import org.jgroups.util.RingBuffer;
import org.jgroups.util.SeqnoList;
import org.jgroups.util.Util;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Bela Ban
 * @since 3.1
 */
@Test(groups=Global.FUNCTIONAL,description="Functional tests for the RingBuffer class")
public class RingBufferTest {

    public void testConstructor() {
        RingBuffer buf=new RingBuffer(100, 1);
        System.out.println("buf = " + buf);
        assert buf.capacity() == 100;
        assert buf.size() == 0;
    }

    public void testIndex() {
        RingBuffer buf=new RingBuffer(10, 5);
        assert buf.getHighestDelivered() == 5;
        assert buf.getHighestReceived() == 5;
        buf.add(6,6); buf.add(7,7);
        buf.remove(); buf.remove();
        long low=buf.getLow();
        buf.stable(4);
        buf.stable(5);
        buf.stable(6);
        buf.stable(7);
        System.out.println("buf = " + buf);
        for(long i=low; i <= 7; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";
    }

    public void testAddWithInvalidSeqno() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(100, 20);
        assert buf.add(10, 0) == false;
        assert buf.add(20, 0) == false;
        assert buf.size() == 0;
    }

    public void testAdd() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        buf.add(1, 322649);
        buf.add(2, 100000);
        System.out.println("buf = " + buf);
        assert buf.size() == 2;
    }

    public void testSaturation() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,3,4,5))
            buf.add(i, i);
        System.out.println("buf = " + buf);
        int size=buf.size(), space_used=buf.spaceUsed();
        double saturation=buf.saturation();
        System.out.println("size=" + size + ", space used=" + space_used + ", saturation=" + saturation);
        assert buf.size() == 5;
        assert buf.spaceUsed() == 5;
        assert buf.saturation() == 0.5;

        buf.remove(); buf.remove(); buf.remove();
        size=buf.size();
        space_used=buf.spaceUsed();
        saturation=buf.saturation();
        System.out.println("size=" + size + ", space used=" + space_used + ", saturation=" + saturation);
        assert buf.size() == 2;
        assert buf.spaceUsed() == 5;
        assert buf.saturation() == 0.5;

        long low=buf.getLow();
        buf.stable(3);
        for(long i=low; i <= 3; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";

        size=buf.size();
        space_used=buf.spaceUsed();
        saturation=buf.saturation();
        System.out.println("size=" + size + ", space used=" + space_used + ", saturation=" + saturation);
        assert buf.size() == 2;
        assert buf.spaceUsed() == 2;
        assert buf.saturation() == 0.2;
    }

    public void testAddWithWrapAround() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 5);
        for(int i=6; i <=15; i++)
            assert buf.add(i, i) : "addition of seqno " + i + " failed";
        System.out.println("buf = " + buf);
        for(int i=0; i < 3; i++) {
            Integer val=buf.remove();
            System.out.println("removed " + val);
            assert val != null;
        }
        System.out.println("buf = " + buf);

        long low=buf.getLow();
        buf.stable(8);
        System.out.println("buf = " + buf);
        assert buf.getLow() == 8;
        for(long i=low; i <= 8; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";

        for(int i=16; i <= 18; i++)
            assert buf.add(i, i);
        System.out.println("buf = " + buf);

        while(buf.remove() != null)
            ;
        System.out.println("buf = " + buf);
        assert buf.size() == 0;
        assert buf.missing() == 0;
        low=buf.getLow();
        buf.stable(18);
        assert buf.getLow() == 18;
        for(long i=low; i <= 18; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";
    }

    public void testAddBeyondCapacity() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i=1; i <=10; i++)
            assert buf.add(i, i);
        System.out.println("buf = " + buf);
    }

    public void testAddMissing() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,4,5,6))
            buf.add(i, i);
        System.out.println("buf = " + buf);
        assert buf.size() == 5 && buf.missing() == 1;

        Integer num=buf.remove();
        assert num == 1;
        num=buf.remove();
        assert num == 2;
        num=buf.remove();
        assert num == null;

        buf.add(3, 3);
        System.out.println("buf = " + buf);
        assert buf.size() == 4 && buf.missing() == 0;

        for(int i=3; i <= 6; i++) {
            num=buf.remove();
            System.out.println("buf = " + buf);
            assert num == i;
        }

        num=buf.remove();
        assert num == null;
    }


    public void testGetMissing() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(30, 0);
        for(int i: Arrays.asList(2,5,10,11,12,13,15,20,28,30))
            buf.add(i, i);
        System.out.println("buf = " + buf);
        int missing=buf.missing();
        System.out.println("missing=" + missing);
        SeqnoList missing_list=buf.getMissing();
        System.out.println("missing_list = " + missing_list);
        assert missing_list.size() == missing;
    }

    public void testGetMissing2() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        buf.add(1,1);
        SeqnoList missing=buf.getMissing();
        System.out.println("missing = " + missing);
        assert missing == null && buf.missing() == 0;

        buf=new RingBuffer<Integer>(10, 0);
        buf.add(10,10);
        missing=buf.getMissing();
        System.out.println("missing = " + missing);
        assert buf.missing() == missing.size();

        buf=new RingBuffer<Integer>(10, 0);
        buf.add(5,5);
        missing=buf.getMissing();
        System.out.println("missing = " + missing);
        assert buf.missing() == missing.size();

        buf=new RingBuffer<Integer>(10, 0);
        buf.add(5,7);
        missing=buf.getMissing();
        System.out.println("missing = " + missing);
        assert buf.missing() == missing.size();
    }

    public void testBlockingAddAndDestroy() {
        final RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i=0; i <= 10; i++)
            buf.add(i, i, true);
        System.out.println("buf = " + buf);
        new Thread() {
            public void run() {
                Util.sleep(1000);
                buf.destroy();
            }
        }.start();
        boolean success=buf.add(11, 11, true);
        System.out.println("buf=" + buf);
        assert !success;
        assert buf.size() == 10;
        assert buf.missing() == 0;
    }

    public void testBlockingAddAndStable() {
        final RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i=0; i <= 10; i++)
            buf.add(i, i, true);
        System.out.println("buf = " + buf);
        new Thread() {
            public void run() {
                Util.sleep(1000);
                for(int i=0; i < 3; i++)
                    buf.remove();
                buf.stable(3);
            }
        }.start();
        boolean success=buf.add(11, 11, true);
        System.out.println("buf=" + buf);
        assert success;
        assert buf.size() == 8;
        assert buf.missing() == 0;
    }

    public void testGet() {
        final RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,3,4,5))
            buf.add(i, i);
        assert buf.get(0) == null;
        assert buf.get(1) == 1;
        assert buf.get(10) == null;
        assert buf.get(5) == 5;
        assert buf.get(6) == null;
    }

    public void testGetList() {
        final RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,3,4,5))
            buf.add(i, i);
        List<Integer> elements=buf.get(3,5);
        System.out.println("elements = " + elements);
        assert elements != null && elements.size() == 3;
        assert elements.contains(3) && elements.contains(4) && elements.contains(5);

        elements=buf.get(4, 10);
        System.out.println("elements = " + elements);
        assert elements != null && elements.size() == 2;
        assert elements.contains(4) && elements.contains(5);

        elements=buf.get(10, 20);
        assert elements == null;
    }

    public void testRemovedPastHighestReceived() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i=1; i <= 15; i++) {
            if(i > 10) {
                assert  !buf.add(i,i);
                Integer num=buf.remove();
                assert num == null;
            }
            else {
                assert  buf.add(i,i);
                Integer num=buf.remove();
                assert num != null && num == i;
            }
        }
        System.out.println("buf = " + buf);
        assert buf.size() == 0;
        assert buf.missing() == 0;
    }

    public void testRemoveMany() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,3,4,5,6,7,9,10))
            buf.add(i, i);
        List<Integer> list=buf.removeMany(false,3);
        System.out.println("list = " + list);
        assert list != null && list.size() == 3;

        list=buf.removeMany(false, 0);
        System.out.println("list = " + list);
        assert list != null && list.size() == 4;

        list=buf.removeMany(false, 10);
        assert list == null;

        buf.add(8, 8);
        list=buf.removeMany(false, 0);
        System.out.println("list = " + list);
        assert list != null && list.size() == 3;
    }


    public void testConcurrentAdd() {
        final int NUM=100;
        final RingBuffer<Integer> buf=new RingBuffer<Integer>(1000, 0);

        CountDownLatch latch=new CountDownLatch(1);
        Adder[] adders=new Adder[NUM];
        for(int i=0; i < adders.length; i++) {
            adders[i]=new Adder(latch, i+1, buf);
            adders[i].start();
        }

        Util.sleep(1000);
        System.out.println("releasing threads");
        latch.countDown();
        System.out.print("waiting for threads to be done: ");
        for(Adder adder: adders) {
            try {
                adder.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("OK");
        System.out.println("buf = " + buf);
        assert buf.size() == NUM;
    }


    public void testStable() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i=1; i <=7; i++) {
            buf.add(i, i);
            buf.remove();
        }
        System.out.println("buf = " + buf);
        assert buf.size() == 0;
        long low=buf.getLow();
        buf.stable(3);
        assert buf.getLow() == 3;
        for(long i=low; i <= 3; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";


        buf.stable(6);
        assert buf.get(6) == null;
        buf.stable(7);
        assert buf.get(7) == null;
        assert buf.getLow() == 7;
        assert buf.size()  == 0;

        for(int i=7; i <= 14; i++) {
            buf.add(i, i);
            buf.remove();
        }

        System.out.println("buf = " + buf);
        assert buf.size() == 0;

        low=buf.getLow();
        buf.stable(12);
        System.out.println("buf = " + buf);
        assert buf.getLow() == 12;
        for(long i=low; i <= 12; i++)
            assert buf._get(i) == null : "message with seqno=" + i + " is not null";
    }
    

    public void testIterator() {
        RingBuffer<Integer> buf=new RingBuffer<Integer>(10, 0);
        for(int i: Arrays.asList(1,2,3,4,5,6,7,9,10))
            buf.add(i, i);
        int count=0;
        for(Integer num: buf) {
            if(num != null) {
                count++;
                System.out.print(num + " ");
            }
        }
        System.out.println();
        assert count == 9 : "count=" + count;
        buf.add(8,8);
        count=0;
        for(Integer num: buf) {
            if(num != null) {
                System.out.print(num + " ");
                count++;
            }
        }
        assert count == 10 : "count=" + count;
    }


    protected static class Adder extends Thread {
        protected final CountDownLatch latch;
        protected final int seqno;
        protected final RingBuffer<Integer> buf;

        public Adder(CountDownLatch latch, int seqno, RingBuffer<Integer> buf) {
            this.latch=latch;
            this.seqno=seqno;
            this.buf=buf;
        }

        public void run() {
            try {
                latch.await();
                Util.sleepRandom(10, 500);
                buf.add(seqno, seqno);
            }
            catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



}