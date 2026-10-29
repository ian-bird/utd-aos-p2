package utd.aos.p2.mutex;

import java.util.List;

import utd.aos.p2.pusher.Pusher;

public class Maekawa implements Mutex {

    public Maekawa(int myId, Pusher<MutexData> incoming, List<Pusher<MutexData>> outgoing, List<Integer> membershipSetIds, MaekawaStrategy deadlockResolution){
        
    }

    @Override
    public void csEnter() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'csEnter'");
    }

    @Override
    public void csLeave() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'csLeave'");
    }
    
}
