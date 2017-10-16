package blog.typed.persistence.scaladsl

import akka.Done
import akka.typed.Behavior
import akka.typed.persistence.scaladsl.PersistentActor
import akka.typed.persistence.scaladsl.PersistentActor.{Actions => State}
import akka.typed.persistence.scaladsl.PersistentActor._
import akka.typed.cluster.sharding.EntityTypeKey

object BlogPost {

  val ShardingTypeName = EntityTypeKey[BlogCommand]("BlogPost")
  val MaxNumberOfShards = 1000

  /**
   * An ordinary persistent actor with a fixed persistenceId,
   * see alternative `shardingBehavior` below.
   */
  def behavior: Behavior[BlogCommand] =
    PersistentActor.immutable[BlogCommand, BlogEvent, BlogData](
      persistenceId = "abc",
      initialState = BlogData.empty,
      states,
      applyEvent)

  /**
   * Persistent actor in Cluster Sharding, when the persistenceId is not known
   * until the actor is started and typically based on the entityId, which
   * is the actor name.
   */
  def shardingBehavior: Behavior[BlogCommand] =
    PersistentActor.persistentEntity[BlogCommand, BlogEvent, BlogData](
      persistenceIdFromActorName = name => ShardingTypeName.name + "-" + name,
      initialState = BlogData.empty,
      states,
      applyEvent)

  private val states: State[BlogCommand, BlogEvent, BlogData] = State.byState {
    case _ : EmptyBlog ⇒ initial
    case _ : BlogWithContent ⇒ postAdded
    case _ : PublishedBlog => postPublished
  }

  private val initial: State[BlogCommand, BlogEvent, BlogData] =
    State { (ctx, cmd, data) ⇒
      cmd match {
        case AddPost(content, replyTo) ⇒
          val evt = PostAdded(content.postId, content)
          Persist(evt).andThen { data2 ⇒
            // After persist is done additional side effects can be performed
            replyTo ! AddPostDone(content.postId)
          }
        case PassivatePost =>
          Stop()
        case other ⇒
          Unhandled()
      }
    }

  private val postAdded: State[BlogCommand, BlogEvent, BlogData] = {
    State { (ctx, cmd, data) ⇒
      cmd match {
        case ChangeBody(newBody, replyTo) ⇒
          val evt = BodyChanged(data.postId, newBody)
          Persist(evt).andThen { _ ⇒
            replyTo ! Done
          }
        case Publish(replyTo) ⇒
          Persist(Published(data.postId)).andThen { _ ⇒
            println(s"Blog post ${data.postId} was published")
            replyTo ! Done
          }
        case GetPost(replyTo) ⇒
          replyTo ! data.content
          PersistNothing()
        case _: AddPost ⇒
          Unhandled()
        case PassivatePost =>
          Stop()
      }
    }
  }

  private val postPublished: State[BlogCommand, BlogEvent, BlogData] = {
    State { (ctx, cmd, data) ⇒
      cmd match {
        case ChangeBody(newBody, replyTo) ⇒
          val evt = BodyChanged(data.postId, newBody)
          Persist(evt).andThen { _ ⇒
            replyTo ! Done
          }
        case Publish(replyTo) ⇒
          Unhandled()
        case GetPost(replyTo) ⇒
          replyTo ! data.content
          PersistNothing()
        case _: AddPost ⇒
          Unhandled()
        case PassivatePost =>
          Stop()
      }
    }
  }

  private def applyEvent(event: BlogEvent, data: BlogData): BlogData =
    event match {
      case PostAdded(postId, content) ⇒
        BlogWithContent(content)

      case BodyChanged(_, newBody) ⇒
        data match {
          case b: EmptyBlog => b
          case b: BlogWithContent =>
            b.copy(content = b.content.copy(body = newBody))
          case b: PublishedBlog =>
            BlogWithContent(content = b.content.copy(body = newBody))
        }

      case Published(_) ⇒
        data match {
          case b: BlogWithContent =>
            PublishedBlog(content = data.content)
        }
    }

}
