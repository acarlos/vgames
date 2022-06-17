package br.com.odvox.vgames.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.servlet.http.HttpServletRequest;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.Voice;
import com.amazonaws.util.IOUtils;

import br.com.odvox.sherlock.SherlockSCE;
import br.com.odvox.sherlock.model.Pergunta;

@ManagedBean
@SessionScoped
public class ControladorJogo implements Serializable{
	
	/**
	 * SERIAL.
	 */
	private static final long serialVersionUID = 8570618332517943515L;

	private List<Pergunta> perguntasJaRealizadas = new ArrayList<Pergunta>();

	private final String INDEX = "/jogo.jsf?faces-redirect=true";

	private AmazonPollyClient polly;
	
	private Voice voice;
	
	private Region region = Region.getRegion(Regions.AP_SOUTHEAST_1);

	private String urlSaudacao;

	private String urlDespedida;

	private String urlPergunta;

	private String textoResposta;

	private String textoMinhaResposta;

	private Integer numeroPergunta;

	private String gameOver;

	private String locutor;

	private int contagemPontos = 0;

	private int contagemErros = 0;

	private int contagem = 0;

	private Pergunta pergunta;

	private SherlockSCE sherlockSCE;

	private Locale locale;
	
	private ResourceBundle bundle;

	private Boolean prosseguirDesligado = Boolean.TRUE;
	
	private Boolean gravadorDesligado = Boolean.TRUE;
	
	private Boolean enviarDesligado = Boolean.TRUE;
	
	public ControladorJogo() {

	}

	@PostConstruct
	private void configurarBean() {
		//Configurado para inglês.
		this.locale = new Locale("pt", "BR");
		//this.locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
		final ResourceBundle bundle = ResourceBundle.getBundle(FacesContext.getCurrentInstance().getApplication().getMessageBundle(),
				this.locale);
		this.bundle = bundle;
		if ((null != this.locale) && (null != this.bundle)) {
			this.sherlockSCE = new SherlockSCE(this.locale.getLanguage(), this.locale.getCountry());
		}

		if (null == this.locutor) {
			if (new Random().nextBoolean()) {
				if (this.locale.getLanguage().equalsIgnoreCase("pt")) {
					this.locutor = "Vitória";
				} else if (this.locale.getLanguage().equalsIgnoreCase("en")) {
					this.locutor = "Joanna";
				}
			} else {
				if (this.locale.getLanguage().equalsIgnoreCase("pt")) {
					this.locutor = "Ricardo";
				} else if (this.locale.getLanguage().equalsIgnoreCase("en")) {
					this.locutor = "Joey";
				}
			}
		}
		
		// create an Amazon Polly client in a specific region
		this.polly = new AmazonPollyClient(new DefaultAWSCredentialsProviderChain(), new ClientConfiguration());
		if (null != this.polly) {
			this.polly.setRegion(this.region);
			// Create describe voices request.
			DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

			// Synchronously ask Amazon Polly to describe available TTS voices.
			DescribeVoicesResult describeVoicesResult = this.polly.describeVoices(describeVoicesRequest);

			for (Voice voz : describeVoicesResult.getVoices()) {
				if (voz.getName().equalsIgnoreCase(this.locutor)) {
					this.voice = voz;
				}
			}
			if (null != this.voice) { 
				this.carregarSaudacao();
			}
		}
		FacesContext.getCurrentInstance().getExternalContext().getSession(true);
	}

/*	@PreDestroy
	public void destroy() {
		this.locale = null;
		this.bundle = null;
		this.sherlockMain = null;
		this.sherlock = null;
		this.locutor = null;
		this.polly = null;
		this.voice = null;
	}*/
	
	public boolean isJogoIniciado() {
		return this.perguntasJaRealizadas.size() > 0;
	}
	
	public void setProsseguirDesligado(Boolean prosseguir) {
		this.prosseguirDesligado = prosseguir;
		if (this.prosseguirDesligado) {
			this.gravadorDesligado = Boolean.FALSE;
		} else {
			this.gravadorDesligado = Boolean.TRUE;
		}
	}
	
	public Boolean getProsseguirDesligado() {
		return this.prosseguirDesligado;
	}	
	
	public Boolean getGravadorDesligado() {
		return gravadorDesligado;
	}

	public void setGravadorDesligado(Boolean gravadorDesligado) {
		this.gravadorDesligado = gravadorDesligado;
	}	

	public Boolean getEnviarDesligado() {
		return enviarDesligado;
	}

	public void setEnviarDesligado(Boolean enviarDesligado) {
		this.enviarDesligado = enviarDesligado;
	}

	public void carregarSaudacaoComPrimeiraPergunta() {

		try {
			String regras = this.bundle.getString("regras");
			String primeiraPergunta = ",";
			this.pergunta = this.sherlockSCE.getPerguntasDoN1().get(new Random().nextInt(13));
			if (null != this.pergunta) {
				primeiraPergunta += ", " + this.pergunta.getPergunta();
				this.perguntasJaRealizadas.add(this.pergunta);
			}
			System.out.println("Primeira pergunta: " + primeiraPergunta);
			System.out.println("Pergunta Sherlock:" + this.pergunta.getPergunta());
			System.out.println("Resposta Sherlock:" + this.pergunta.getResposta().getSentenca());
			this.setUrlSaudacao(this.getVozURL(regras + ", " + primeiraPergunta + ", "));
			this.setGameOver("jogo");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	public void carregarSaudacao() {
		try {
			String regras = this.bundle.getString("regras");
			this.setUrlSaudacao(this.getVozURL(regras));
			this.setGameOver("jogo");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	public String getVozURL(final String texto) throws UnsupportedEncodingException {
		InputStream speechStream = null;
		try {
			speechStream = synthesize(texto, OutputFormat.Mp3, this.voice, this.polly);
			byte[] bytes = IOUtils.toByteArray(speechStream);
			String vozBase64 = Base64.getEncoder().encodeToString(bytes);
			return "data:audio/mp3;base64," + vozBase64;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private InputStream synthesize(String text, OutputFormat format, Voice voice, AmazonPollyClient polly)
			throws IOException {
		SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest().withText(text).withVoiceId(voice.getId())
				.withOutputFormat(format);
		SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
		return synthRes.getAudioStream();
	}

	/**
	 * @return the urlSaudacao
	 */
	public String getUrlSaudacao() {

		return this.urlSaudacao;
	}

	/**
	 * @param urlSaudacao
	 *            the urlSaudacao to set
	 */
	public void setUrlSaudacao(String urlSaudacao) {

		this.urlSaudacao = urlSaudacao;
	}

	/**
	 * @return the urlDespedida
	 */
	public String getUrlDespedida() {

		return this.urlDespedida;
	}

	/**
	 * @param urlDespedida
	 *            the urlDespedida to set
	 */
	public void setUrlDespedida(String urlDespedida) {

		this.urlDespedida = urlDespedida;
	}

	/**
	 * @return the urlPergunta
	 */
	public String getUrlPergunta() {

		return this.urlPergunta;
	}

	/**
	 * @param urlPergunta
	 *            the urlPergunta to set
	 */
	public void setUrlPergunta(String urlPergunta) {

		this.urlPergunta = urlPergunta;
	}

	/**
	 * @return the textoResposta
	 */
	public String getTextoResposta() {

		if ((null != this.textoResposta) && (!"".equals(this.textoResposta))) {
			return this.textoResposta;
		}
		return "";
	}

	/**
	 * @return the textoResposta
	 */
	public String getTextoMinhaResposta() {

		if ((null != this.textoMinhaResposta) && (!"".equals(this.textoMinhaResposta))) {
			return this.textoMinhaResposta;
		}
		return "";
	}
	
	public void textoInVoice(AjaxBehaviorEvent ev ) {
		this.textoMinhaResposta = (String)((UIOutput)ev.getSource()).getValue();
	}

	public synchronized void ajaxListener(AjaxBehaviorEvent event) {
		this.setProsseguirDesligado(Boolean.FALSE);
		if (null != this.getTextoResposta() && !"".equals(this.getTextoResposta())) {
			try {
				Boolean respostaSherlock = Boolean.FALSE;
				if (null != this.getTextoResposta()) {
					respostaSherlock = this.sherlockSCE.responda(this.getTextoResposta(), this.pergunta);
					this.perguntasJaRealizadas.add(this.pergunta);
					System.out.println("Resposta aferida pergunta " + respostaSherlock);
				}
				String respostaComProximaPergunta = "";
				if ((null != respostaSherlock) && (!respostaSherlock)) {
					// Contabilizar erro.
					respostaComProximaPergunta += ", " + this.sherlockSCE.getMensagemRespostaErrada()
							+ this.pergunta.getResposta().getSentenca() + ", ";
					this.contagemErros++;
				} else {
					// Contabilizar pontos.
					respostaComProximaPergunta += ", " + this.sherlockSCE.getMensagemRespostaCorreta()
							+ this.pergunta.getResposta().getSentenca() + ", ";
					this.contagemPontos = this.contagemPontos + 10;

				}
				this.contagem++;
				this.setNumeroPergunta(this.contagem);
				this.textoMinhaResposta = this.pergunta.getResposta().getSentenca();
				this.pergunta = this.sherlockSCE.getPerguntasDoN1().get(new Random().nextInt(12));
				while (this.perguntasJaRealizadas.contains(this.pergunta)) {
					this.pergunta = this.sherlockSCE.getPerguntasDoN1().get(new Random().nextInt(12));
				}
				if ((this.contagem < 5) && (this.contagemErros < 3)) {
					respostaComProximaPergunta += ", " + this.bundle.getString("proximaPergunta") + ", "
							+ this.pergunta.getPergunta() + ", ";
				} else {
					respostaComProximaPergunta += ", ";
				}

				// System.out.println("Pergunta pergunta " + this.sherlock.getPergunta());
				// System.out.println("Resposta pergunta " + this.sherlock.getResposta().getSentenca());
				if ((null != respostaComProximaPergunta) && (!"".equals(respostaComProximaPergunta))) {
					this.setUrlPergunta(this.getVozURL(respostaComProximaPergunta));
				}

				if ((this.contagem == 5) || (this.contagemErros == 3)) {

					String despedida = this.bundle.getString("voceFez") + " " + this.contagemPontos + " "
							+ this.bundle.getString("pontos") + this.bundle.getString("fimDeJogo");

					this.setUrlDespedida(this.getVozURL(despedida));
					this.setGameOver("fimDeJogo");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			String primeiraPergunta = ",";
			this.pergunta = this.sherlockSCE.getPerguntasDoN1().get(new Random().nextInt(13));
			if (null != this.pergunta) {
				primeiraPergunta += ", " + this.pergunta.getPergunta();
				this.perguntasJaRealizadas.add(this.pergunta);
				try {
					this.setUrlPergunta(this.getVozURL(this.pergunta.getPergunta()));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("Primeira pergunta: " + primeiraPergunta);
			System.out.println("Pergunta Sherlock:" + this.pergunta.getPergunta());
			System.out.println("Resposta Sherlock:" + this.pergunta.getResposta().getSentenca());
		}
		this.setEnviarDesligado(Boolean.TRUE);

	}

	/**
	 * @param textoResposta
	 *            the textoResposta to set
	 */
	public void setTextoResposta(String textoResposta) {

		this.textoResposta = textoResposta;
	}

	public String terminarSessao() {
		this.setTextoResposta("");
		this.textoMinhaResposta = "";
		this.pergunta = null;
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		return this.INDEX;
	}

	public String getGameOver() {

		return this.gameOver;
	}

	public void setGameOver(String gameOver) {

		this.gameOver = gameOver;
	}

	/**
	 * @return the numeroPergunta
	 */
	public Integer getNumeroPergunta() {

		if (this.numeroPergunta == null) {
			return 1;
		}

		return this.numeroPergunta;
	}

	/**
	 * @param numeroPergunta
	 *            the numeroPergunta to set
	 */
	public void setNumeroPergunta(Integer numeroPergunta) {

		this.numeroPergunta = numeroPergunta;
	}
}
