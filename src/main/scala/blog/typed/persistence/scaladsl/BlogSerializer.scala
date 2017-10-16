package blog.typed.persistence.scaladsl

import java.io.NotSerializableException

import akka.serialization.{BaseSerializer, SerializerWithStringManifest}
import akka.typed.cluster.ActorRefResolver
import akka.typed.scaladsl.adapter._
import blog.typed.persistence.scaladsl.protobuf.BlogPostMessages

class BlogSerializer(val system: akka.actor.ExtendedActorSystem)
  extends SerializerWithStringManifest with BaseSerializer {

  private val resolver = ActorRefResolver(system.toTyped)

  private val BlogWithContentManifest = "aa"
  private val PublishedBlogDataManifest = "ab"
  private val PostContentManifest = "ba"
  private val AddPostManifest = "bb"
  private val AddPostDoneManifest = "bc"
  private val ChangeBodyManifest = "bd"
  private val PublishManifest = "ca"
  private val PostAddedManifest = "cb"
  private val BodyChangedManifest = "cd"
  private val PublishedManifest = "da"

  override def manifest(o: AnyRef): String = o match {
    case _: BlogWithContent ⇒ BlogWithContentManifest
    case _: PublishedBlog ⇒ PublishedBlogDataManifest
    case _: PostContent ⇒ PostContentManifest
    case _: AddPost ⇒ AddPostManifest
    case _: AddPostDone ⇒ AddPostDoneManifest
    case _: ChangeBody ⇒ ChangeBodyManifest
    case _: Publish ⇒ PublishManifest
    case _: PostAdded ⇒ PostAddedManifest
    case _: BodyChanged ⇒ BodyChangedManifest
    case _: Published ⇒ PublishedManifest
    case _ ⇒
      throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass} in [${getClass.getName}]")
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case a: BlogData ⇒ blogStateToBinary(a)
    case a: PostContent ⇒ postContentToBinary(a)
    case a: AddPost ⇒ addPostToBinary(a)
    case a: AddPostDone ⇒ addPostDoneToBinary(a)
    case a: ChangeBody ⇒ changeBodyToBinary(a)
    case a: Publish ⇒ publishToBinary(a)
    case a: PostAdded ⇒ postAddedToBinary(a)
    case a: BodyChanged ⇒ bodyChangedToBinary(a)
    case a: Published ⇒ publishedToBinary(a)

    case _ ⇒
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }

  private def blogStateToBinary(a: BlogData): Array[Byte] = {
    a match {
      case b: BlogWithContent =>
        val builder = BlogPostMessages.BlogWithContentData.newBuilder()
        builder.setContent(postContentToProto(b.content))
        builder.build().toByteArray
      case b: PublishedBlog =>
        val builder = BlogPostMessages.PublishedBlogData.newBuilder()
        builder.setContent(postContentToProto(b.content))
        builder.build().toByteArray
    }
  }

  private def postContentToBinary(a: PostContent): Array[Byte] = {
    postContentToProto(a).build().toByteArray()
  }

  private def postContentToProto(a: PostContent): BlogPostMessages.PostContent.Builder = {
    val builder = BlogPostMessages.PostContent.newBuilder()
    builder.setPostId(a.postId).setTitle(a.title).setBody(a.body)
    builder
  }

  private def addPostToBinary(a: AddPost): Array[Byte] = {
    val builder = BlogPostMessages.AddPost.newBuilder()
    builder.setContent(postContentToProto(a.content))
    builder.setReplyTo(resolver.toSerializationFormat(a.replyTo))
    builder.build().toByteArray()
  }

  private def addPostDoneToBinary(a: AddPostDone): Array[Byte] = {
    val builder = BlogPostMessages.AddPostDone.newBuilder()
    builder.setPostId(a.postId)
    builder.build().toByteArray()
  }

  private def changeBodyToBinary(a: ChangeBody): Array[Byte] = {
    val builder = BlogPostMessages.ChangeBody.newBuilder()
    builder.setNewBody(a.newBody)
    builder.setReplyTo(resolver.toSerializationFormat(a.replyTo))
    builder.build().toByteArray()
  }

  private def publishToBinary(a: Publish): Array[Byte] = {
    val builder = BlogPostMessages.Publish.newBuilder()
    builder.setReplyTo(resolver.toSerializationFormat(a.replyTo))
    builder.build().toByteArray()
  }

  private def postAddedToBinary(a: PostAdded): Array[Byte] = {
    val builder = BlogPostMessages.PostAdded.newBuilder()
    builder.setPostId(a.postId).setContent(postContentToProto(a.content))
    builder.build().toByteArray()
  }

  private def bodyChangedToBinary(a: BodyChanged): Array[Byte] = {
    val builder = BlogPostMessages.BodyChanged.newBuilder()
    builder.setPostId(a.postId).setNewBody(a.newBody)
    builder.build().toByteArray()
  }

  private def publishedToBinary(a: Published): Array[Byte] = {
    val builder = BlogPostMessages.Published.newBuilder()
    builder.setPostId(a.postId)
    builder.build().toByteArray()
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
    case BlogWithContentManifest ⇒ blogWithContentFromBinary(bytes)
    case PublishedBlogDataManifest ⇒ publishedBlogFromBinary(bytes)
    case PostContentManifest ⇒ postContentFromBinary(bytes)
    case AddPostManifest ⇒ addPostFromBinary(bytes)
    case AddPostDoneManifest ⇒ addPostDoneFromBinary(bytes)
    case ChangeBodyManifest ⇒ changeBodyFromBinary(bytes)
    case PublishManifest ⇒ publishFromBinary(bytes)
    case PostAddedManifest ⇒ postAddedFromBinary(bytes)
    case BodyChangedManifest ⇒ bodyChangedFromBinary(bytes)
    case PublishedManifest ⇒ publishedFromBinary(bytes)

    case _ ⇒
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]")
  }

  private def blogWithContentFromBinary(bytes: Array[Byte]): BlogData = {
    val a = BlogPostMessages.BlogWithContentData.parseFrom(bytes)
    if (a.hasContent) {
      val c = a.getContent
      BlogWithContent(PostContent(c.getPostId, c.getTitle, c.getBody))
    } else EmptyBlog()
  }

  private def publishedBlogFromBinary(bytes: Array[Byte]): BlogData = {
    val a = BlogPostMessages.PublishedBlogData.parseFrom(bytes)
    if (a.hasContent) {
      val c = a.getContent
      PublishedBlog(PostContent(c.getPostId, c.getTitle, c.getBody))
    } else EmptyBlog()
  }

  private def postContentFromBinary(bytes: Array[Byte]): PostContent = {
    val a = BlogPostMessages.PostContent.parseFrom(bytes)
    PostContent(a.getPostId, a.getTitle, a.getBody)
  }

  private def addPostFromBinary(bytes: Array[Byte]): AddPost = {
    val a = BlogPostMessages.AddPost.parseFrom(bytes)
    val c = a.getContent
    AddPost(
      PostContent(c.getPostId, c.getTitle, c.getBody),
      resolver.resolveActorRef(a.getReplyTo))
  }

  private def addPostDoneFromBinary(bytes: Array[Byte]): AddPostDone = {
    val a = BlogPostMessages.AddPostDone.parseFrom(bytes)
    AddPostDone(a.getPostId)
  }

  private def changeBodyFromBinary(bytes: Array[Byte]): ChangeBody = {
    val a = BlogPostMessages.ChangeBody.parseFrom(bytes)
    ChangeBody(a.getNewBody, resolver.resolveActorRef(a.getReplyTo))
  }

  private def publishFromBinary(bytes: Array[Byte]): Publish = {
    val a = BlogPostMessages.Publish.parseFrom(bytes)
    Publish(resolver.resolveActorRef(a.getReplyTo))
  }

  private def postAddedFromBinary(bytes: Array[Byte]): PostAdded = {
    val a = BlogPostMessages.PostAdded.parseFrom(bytes)
    val c = a.getContent
    PostAdded(a.getPostId, PostContent(c.getPostId, c.getTitle, c.getBody))
  }

  private def bodyChangedFromBinary(bytes: Array[Byte]): BodyChanged = {
    val a = BlogPostMessages.BodyChanged.parseFrom(bytes)
    BodyChanged(a.getPostId, a.getNewBody)
  }

  private def publishedFromBinary(bytes: Array[Byte]): Published = {
    val a = BlogPostMessages.Published.parseFrom(bytes)
    Published(a.getPostId)
  }

}
