package blog.typed.persistence.scaladsl

import akka.typed.persistence.scaladsl.PersistentActor
import akka.typed.Behavior

object BlogPost1 {

  def behavior: Behavior[BlogCommand] =
    PersistentActor.immutable[BlogCommand, BlogEvent, BlogData](
      persistenceId = "abc",
      initialState = BlogData.empty,
      actions = PersistentActor.Actions { (ctx, cmd, state) ⇒ ??? },
      applyEvent = (evt, state) ⇒ ???)

}

