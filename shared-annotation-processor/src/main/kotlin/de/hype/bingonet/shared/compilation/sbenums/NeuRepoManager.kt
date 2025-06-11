package de.hype.bingonet.shared.compilation.sbenums

import de.hype.bingonet.shared.compilation.Main.PROJECT_DIR
import io.github.moulberry.repo.NEURepository
import io.github.moulberry.repo.data.NEUItem
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import java.io.File
import java.io.IOException
import java.nio.file.Path

object NeuRepoManager {
    val LOCAL_PATH: Path = File("${PROJECT_DIR}/neu-repo").toPath()
    private const val REPO_URL = "https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git"
    private val repository: NEURepository = NEURepository.of(LOCAL_PATH)

    val items: Map<String, NEUItem> by lazy {
        repository.getItems().items
    }

    init {
        try {
            updateRepo()
        } catch (e: Exception) {
        }
        repository.reload()
    }

    @Throws(GitAPIException::class, IOException::class)
    private fun updateRepo() {
        val repoDir = LOCAL_PATH.toFile()
        if (!repoDir.exists() || (repoDir.isDirectory && repoDir.listFiles()!!.size == 0)) {
            repoDir.mkdirs()
            cloneRepo(repoDir)
        } else {
            fetchChanges(repoDir)
        }
    }

    @Throws(GitAPIException::class)
    private fun cloneRepo(repoDir: File?) {
        println("BingoNet Server: Cloning NEU Repo: Start")
        Git.cloneRepository()
            .setURI(REPO_URL)
            .setDirectory(repoDir)
            .call()
        println("BingoNet Server: Cloning NEU Repo: Done")
    }

    @Throws(IOException::class, GitAPIException::class)
    private fun fetchChanges(repoDir: File?) {
        Git.open(repoDir).use { git ->
            println("BingoNet Server: Fetching latest NEU Repo changes...")
            git.fetch().call()
            println("BingoNet Server: Checking out the latest NEU Repo changes...")
            git.pull().call()
            println("BingoNet Server: NEU Repo updated.")
        }
    }
}
