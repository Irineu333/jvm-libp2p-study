import io.libp2p.core.PeerId
import io.libp2p.core.dsl.host
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.discovery.MDnsDiscovery
import io.libp2p.protocol.Ping
import java.net.Inet4Address

fun main() {

    val address = Inet4Address.getByName("0.0.0.0")

    val node = host {
        identity { random() }
        protocols {
            +Ping()
        }
        network {
            listen("/ip4/${address.hostAddress}/tcp/0")
        }
    }

    node.start().get()

    println()
    println("Node started")
    println("id: ${node.peerId}")
    println("address: ${node.listenAddresses().single()}")
    println()

    val peerFinder = MDnsDiscovery(node, address = address)

    peerFinder.newPeerFoundListeners += newPeer@{ peerInfo ->

        if (peerInfo.peerId == node.peerId) return@newPeer

        node.network.connect(
            peerInfo.addresses.first().withP2P(peerInfo.peerId)
        )
    }

    peerFinder.start()

    do {

        println("1. Connect to peer")
        println("2. List peers")
        println("3. Ping")
        println("4. Chat")
        println("0. Exit")
        print("> ")

        val op = readln().toInt()

        println()

        when (op) {
            1 -> {
                print("Peer address: ")
                val address = Multiaddr(readln())
                val connection = node.network.connect(address).get()
                println("Connected to ${connection.secureSession().remoteId}")
            }

            2 -> {
                val connections = node.network.connections.takeIf { it.isNotEmpty() }

                connections?.let {
                    println("Peers:")
                    connections.forEach {
                        println("${it.secureSession().remoteId}, isInitiator = ${it.isInitiator}")
                    }
                } ?: println("No peers connected")
            }

            3 -> {
                print("Peer: ")
                val peerId = readln()

                Ping().dial(node, PeerId.fromBase58(peerId))
                    .controller
                    .thenCompose { controller ->
                        controller.ping()
                    }.thenAccept { latency ->
                        println("Ping: $latency ms")
                    }.join()
            }

            4 -> {

                val connections = node.network.connections.distinctBy {
                    it.secureSession().remoteId
                }

                if (connections.isEmpty()) {
                    println("No peers connected")
                } else {

                    println("Chat initiated.")
                    println("Type 'exit' to quit.")

                    val chat = Chat(
                        onMessage = { peerId, message ->
                            println("$peerId > $message")
                        }
                    )

                    node.addProtocolHandler(chat)

                    do {
                        val message = readlnOrNull().orEmpty()

                        connections.map { connection ->
                            node.newStream<ChatController>(
                                listOf("chat/0.1.0"),
                                connection
                            )
                        }.forEach { chat ->
                            chat.controller.thenAccept {
                                it.send(message)
                            }
                        }
                    } while (message != "exit")

                    node.removeProtocolHandler(chat)
                }
            }
        }

        println()

    } while (op != 0)

    node.stop().get()
    println("Node stopped")
}
