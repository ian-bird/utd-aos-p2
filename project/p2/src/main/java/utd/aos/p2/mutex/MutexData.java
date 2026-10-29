package utd.aos.p2.mutex;

import java.io.Serializable;

public sealed interface MutexData extends Serializable permits Request, Grant, Inquire, Release, Failed, Yield {
    
}
